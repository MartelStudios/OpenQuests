package com.martelstudios.openquests.core.persistence.jdbc;

import com.hypixel.hytale.logger.HytaleLogger;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.AbstractQuestProgression.Stored;
import com.martelstudios.openquests.core.models.AssignmentRecord;
import com.martelstudios.openquests.core.models.AssignmentRecords;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.persistence.CodecJson;
import com.martelstudios.openquests.core.persistence.PlayerMessage;
import com.martelstudios.openquests.core.persistence.PlayerQuestRecord;
import com.martelstudios.openquests.core.persistence.QuestProgressionRecord;
import com.martelstudios.openquests.core.persistence.QuestStorage;
import com.martelstudios.openquests.core.persistence.QuestStorageException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.sql.Types;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

/**
 * The quests in a relational database, for a server keeping tens of thousands of them or for
 * several servers sharing them.
 *
 * <p>A quest is one row holding its whole document and a version that grows with every write, and
 * a row is only ever written over the version its writer read: a write another one overtook is
 * refused, for the caller to make again on what overtook it. Who holds a quest is a table of its
 * own, written with the row from the same document, which makes "the quests of this player" an
 * index lookup rather than a scan.
 */
public class JdbcQuestStorage implements QuestStorage {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final String ID = "Jdbc";

    /**
     * The layout this version writes. Tables written by another are refused rather than misread.
     */
    private static final int SCHEMA_VERSION = 6;

    /**
     * How many ids go into one {@code IN (…)} list, past which another round trip is cheaper.
     */
    private static final int IN_CLAUSE_CHUNK = 500;

    private static final String QUEST_COLUMNS = "id, asset_id, state, completed_at, data, version, updated_at, created_at";

    /**
     * How long an ended quest nobody holds keeps its outcome, for servers that have not heard yet.
     */
    private static final Duration ENDED_KEPT_FOR = Duration.ofDays(1);

    private static final String RUNNING = QuestState.IN_PROGRESS.name();

    /**
     * How long a player moving in waits for the server they come from to write them out and let go.
     */
    private static final Duration HANDOFF_WAIT = Duration.ofSeconds(10);

    private static final Duration HANDOFF_POLL = Duration.ofMillis(250);

    private final JdbcSettings settings;
    private final SqlDialect dialect;

    /**
     * Names this server while it runs, in the rows of the players it hosts: a server started again
     * is another, whose players wait for the last one's hold to run out.
     */
    private final String hostId = UUID.randomUUID().toString();

    /**
     * How long hosting a player holds without being renewed, until the first renewal says.
     */
    private volatile long hostingMillis = Duration.ofMinutes(1).toMillis();

    private JdbcDriverLoader driverLoader;
    private JdbcConnectionPool pool;

    private String questTable;
    private String questPlayerTable;
    private String questIndexTable;
    private String playerTable;
    private String messageTable;
    private String assignmentTable;
    private String versionTable;

    @Nullable
    private String insertMemberSql;

    @Nullable
    private String insertLinkSql;

    /**
     * Inserts a quest unless another write inserted it first, {@code null} where the database has
     * no such statement.
     */
    @Nullable
    private String insertQuestSql;

    public JdbcQuestStorage(@Nonnull JdbcSettings settings) {
        this.settings = settings;
        this.dialect = settings.dialect();
    }

    @Override
    public void start() {
        String prefix = settings.tablePrefix();

        questTable = prefix + "quest";
        questPlayerTable = prefix + "quest_player";
        questIndexTable = prefix + "quest_index";
        playerTable = prefix + "player";
        messageTable = prefix + "player_message";
        assignmentTable = prefix + "quest_assignment";
        versionTable = prefix + "schema_version";

        insertMemberSql = dialect.insertIgnoring(questIndexTable, "index_key, quest_id");
        insertLinkSql = dialect.insertIgnoring(questPlayerTable, "quest_id, player_id, abandoned");
        insertQuestSql = dialect.insertIgnoring(questTable, QUEST_COLUMNS);

        driverLoader = JdbcDriverLoader.load(settings.url(), settings.driverPath(), settings.driverClass());

        Properties properties = new Properties();
        if (settings.user() != null) properties.setProperty("user", settings.user());
        if (settings.password() != null) properties.setProperty("password", settings.password());

        pool = new JdbcConnectionPool(driverLoader.getDriver(), settings.url(), properties, settings.poolSize(), settings.connectionTimeoutSeconds());

        prepareSchema();
        dropLongEnded();

        LOGGER.atInfo().log("Quest storage: %s as %s", settings.url(), dialect);
    }

    @Override
    public void close() {
        if (pool != null) pool.close();
        if (driverLoader != null) driverLoader.close();
    }

    @Nonnull
    @Override
    public String getId() {
        return ID;
    }

    @Override
    public boolean isShared() {
        return true;
    }

    @Nullable
    @Override
    public AbstractQuestProgression<?> loadProgression(@Nonnull UUID questId) {
        List<AbstractQuestProgression<?>> quests = loadProgressions(List.of(questId));
        return quests.isEmpty() ? null : quests.getFirst();
    }

    @Nonnull
    @Override
    public List<AbstractQuestProgression<?>> loadProgressions(@Nonnull Collection<UUID> questIds) {
        if (questIds.isEmpty()) return List.of();

        List<UUID> ids = new ArrayList<>(questIds);

        return pool.with(connection -> {
            List<AbstractQuestProgression<?>> quests = new ArrayList<>();

            for (int from = 0; from < ids.size(); from += IN_CLAUSE_CHUNK) {
                List<UUID> chunk = ids.subList(from, Math.min(from + IN_CLAUSE_CHUNK, ids.size()));

                try (PreparedStatement statement = connection.prepareStatement("SELECT id, data, version FROM " + questTable + " WHERE id IN (" + placeholders(chunk.size()) + ")")) {
                    bindIds(statement, 1, chunk);
                    readQuests(statement, quests);
                }
            }
            return quests;
        });
    }

    @Nonnull
    @Override
    public List<AbstractQuestProgression<?>> loadPlayerProgressions(@Nonnull UUID playerId) {
        return pool.with(connection -> {
            String sql = "SELECT q.id, q.data, q.version FROM " + questTable + " q JOIN " + questPlayerTable + " l ON l.quest_id = q.id WHERE l.player_id = ?";

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, playerId.toString());

                List<AbstractQuestProgression<?>> quests = new ArrayList<>();
                readQuests(statement, quests);

                List<AbstractQuestProgression<?>> held = new ArrayList<>();
                for (AbstractQuestProgression<?> quest : quests) {
                    if (quest.getPlayers().contains(playerId) || quest.getAbandonedPlayers().contains(playerId)) held.add(quest);
                }
                return held;
            }
        });
    }

    @Nonnull
    @Override
    public List<AbstractQuestProgression<?>> loadAllProgressions() {
        return pool.with(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("SELECT id, data, version FROM " + questTable)) {
                List<AbstractQuestProgression<?>> quests = new ArrayList<>();
                readQuests(statement, quests);
                return quests;
            }
        });
    }

    /**
     * One transaction, whatever the number of quests: each written over the version it stands on,
     * with who holds it. A commit whose answer is lost on the way is looked up rather than reported
     * failed, which would have a change made twice.
     */
    @Nonnull
    @Override
    public Set<UUID> writeProgressions(@Nonnull Collection<? extends AbstractQuestProgression<?>> quests) {
        if (quests.isEmpty()) return Set.of();

        List<Snapshot> snapshots = new ArrayList<>(quests.size());
        for (AbstractQuestProgression<?> quest : quests) snapshots.add(Snapshot.of(quest));

        List<Snapshot> written;
        try {
            written = pool.inTransaction(connection -> {
                List<Snapshot> rows = writeRows(connection, snapshots, System.currentTimeMillis());
                relink(connection, rows);
                return rows;
            });
        } catch (QuestStorageException e) {
            written = writtenAmong(snapshots);
            if (written.isEmpty()) throw e;
        }

        Set<UUID> moved = new HashSet<>();
        for (Snapshot snapshot : snapshots) moved.add(snapshot.quest().getId());

        for (Snapshot snapshot : written) {
            snapshot.quest().markStored(snapshot.written());
            moved.remove(snapshot.quest().getId());
        }
        return moved;
    }

    /**
     * Writes the rows alone, each over the version it stands on.
     *
     * @return those written, leaving out a row another write moved and one inserted first elsewhere
     */
    @Nonnull
    private List<Snapshot> writeRows(@Nonnull Connection connection, @Nonnull List<Snapshot> snapshots, long now) throws SQLException {
        List<Snapshot> written = new ArrayList<>();
        List<Snapshot> updates = new ArrayList<>();

        for (Snapshot snapshot : snapshots) {
            if (snapshot.version() > 0) {
                updates.add(snapshot);
            } else if (insert(connection, snapshot, now)) {
                written.add(snapshot);
            }
        }
        if (updates.isEmpty()) return written;

        try (PreparedStatement update = connection.prepareStatement("UPDATE " + questTable + " SET state = ?, completed_at = ?, data = ?, version = ?, updated_at = ? WHERE id = ? AND version = ?")) {
            for (Snapshot snapshot : updates) {
                bindRow(update, snapshot, snapshot.version() + 1, now);
                update.setString(6, snapshot.id());
                update.setLong(7, snapshot.version());
                update.addBatch();
            }

            int[] counts = update.executeBatch();
            for (int i = 0; i < counts.length; i++) {
                // A driver that does not count batched rows says so, and the row tells what happened
                if (counts[i] == 1 || counts[i] == Statement.SUCCESS_NO_INFO && holds(connection, updates.get(i))) written.add(updates.get(i));
            }
        }
        return written;
    }

    /**
     * @return {@code false} if another write inserted the quest first.
     */
    private boolean insert(@Nonnull Connection connection, @Nonnull Snapshot snapshot, long now) throws SQLException {
        if (insertQuestSql == null && readVersion(connection, snapshot.id()) != null) return false;

        String sql = insertQuestSql != null ? insertQuestSql : "INSERT INTO " + questTable + " (" + QUEST_COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement insert = connection.prepareStatement(sql)) {
            insert.setString(1, snapshot.id());
            insert.setString(2, snapshot.assetId());
            insert.setString(3, snapshot.state().name());
            bindInstant(insert, 4, snapshot.completedAt());
            insert.setString(5, snapshot.data());
            insert.setLong(6, 1);
            insert.setLong(7, now);
            insert.setLong(8, now);
            return insert.executeUpdate() == 1;
        }
    }

    /**
     * @return those whose row stands at the version they were written as, holding exactly their
     * document, which only their write could have left. None when the database cannot say.
     */
    @Nonnull
    private List<Snapshot> writtenAmong(@Nonnull List<Snapshot> snapshots) {
        try {
            return pool.with(connection -> {
                List<Snapshot> written = new ArrayList<>();
                for (Snapshot snapshot : snapshots) {
                    if (holds(connection, snapshot)) written.add(snapshot);
                }
                return written;
            });
        } catch (QuestStorageException e) {
            return List.of();
        }
    }

    private boolean holds(@Nonnull Connection connection, @Nonnull Snapshot snapshot) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT data FROM " + questTable + " WHERE id = ? AND version = ?")) {
            statement.setString(1, snapshot.id());
            statement.setLong(2, snapshot.version() + 1);

            try (ResultSet results = statement.executeQuery()) {
                return results.next() && snapshot.data().equals(results.getString(1));
            }
        }
    }

    @Nonnull
    @Override
    public Map<UUID, Long> loadVersions(@Nonnull Collection<UUID> questIds) {
        if (questIds.isEmpty()) return Map.of();

        List<UUID> ids = new ArrayList<>(questIds);

        return pool.with(connection -> {
            Map<UUID, Long> versions = new HashMap<>();

            for (int from = 0; from < ids.size(); from += IN_CLAUSE_CHUNK) {
                List<UUID> chunk = ids.subList(from, Math.min(from + IN_CLAUSE_CHUNK, ids.size()));

                try (PreparedStatement statement = connection.prepareStatement("SELECT id, version FROM " + questTable + " WHERE id IN (" + placeholders(chunk.size()) + ")")) {
                    bindIds(statement, 1, chunk);

                    try (ResultSet results = statement.executeQuery()) {
                        while (results.next()) {
                            UUID questId = parseUuid(results.getString(1));
                            if (questId != null) versions.put(questId, results.getLong(2));
                        }
                    }
                }
            }
            return versions;
        });
    }

    /**
     * An ended quest keeps its row, its end written first if the row does not say it yet, for {@link
     * #ENDED_KEPT_FOR}: deleted outright, it would read as never written to a server still running
     * it, which would then end it anew.
     */
    @Override
    public void deleteProgression(@Nonnull AbstractQuestProgression<?> quest) {
        String questId = quest.getId().toString();
        boolean ended = quest.isCompleted();

        // A quest never written has nobody to tell
        boolean untold = quest.getStoredVersion() > 0 && (quest.hasChanges() || !quest.getStoredState().isCompleted());
        Snapshot snapshot = ended && untold ? Snapshot.of(quest) : null;

        pool.inTransaction(connection -> {
            execute(connection, "DELETE FROM " + questPlayerTable + " WHERE quest_id = ?", questId);
            execute(connection, "DELETE FROM " + questIndexTable + " WHERE quest_id = ?", questId);

            if (!ended) {
                execute(connection, "DELETE FROM " + questTable + " WHERE id = ?", questId);
            } else if (snapshot != null) {
                writeRows(connection, List.of(snapshot), System.currentTimeMillis());
            }
            return null;
        });
    }

    /**
     * Drops what is left of the quests that ended long enough ago for every server to have heard,
     * and that nobody holds any more.
     */
    private void dropLongEnded() {
        long before = System.currentTimeMillis() - ENDED_KEPT_FOR.toMillis();
        String longEnded = "SELECT id FROM " + questTable + " WHERE state <> ? AND completed_at < ? AND id NOT IN (SELECT quest_id FROM " + questPlayerTable + ")";

        pool.inTransaction(connection -> {
            try (PreparedStatement delete = connection.prepareStatement("DELETE FROM " + questIndexTable + " WHERE quest_id IN (" + longEnded + ")")) {
                delete.setString(1, RUNNING);
                delete.setLong(2, before);
                delete.executeUpdate();
            }
            try (PreparedStatement delete = connection.prepareStatement("DELETE FROM " + questTable + " WHERE state <> ? AND completed_at < ? AND id NOT IN (SELECT quest_id FROM " + questPlayerTable + ")")) {
                delete.setString(1, RUNNING);
                delete.setLong(2, before);
                delete.executeUpdate();
            }
            return null;
        });
    }

    /**
     * Who holds each quest, moved from who held the version written over: the links of a row always
     * follow its document, being written with it, so only the players who moved are written.
     */
    private void relink(@Nonnull Connection connection, @Nonnull List<Snapshot> snapshots) throws SQLException {
        try (PreparedStatement deleteLink = connection.prepareStatement("DELETE FROM " + questPlayerTable + " WHERE quest_id = ? AND player_id = ?");
             PreparedStatement insertLink = connection.prepareStatement(insertLinkSql())) {

            int deletes = 0;
            int inserts = 0;
            for (Snapshot snapshot : snapshots) {
                for (UUID playerId : snapshot.movedHolders()) {
                    deleteLink.setString(1, snapshot.id());
                    deleteLink.setString(2, playerId.toString());
                    deleteLink.addBatch();
                    deletes++;

                    if (snapshot.players().contains(playerId)) bindLink(insertLink, snapshot.id(), playerId, false);
                    if (snapshot.abandoned().contains(playerId)) bindLink(insertLink, snapshot.id(), playerId, true);
                    if (snapshot.players().contains(playerId) || snapshot.abandoned().contains(playerId)) inserts++;
                }
            }

            // Deletes first: a link still standing is one the insert would collide with
            if (deletes > 0) deleteLink.executeBatch();
            if (inserts > 0) insertLink.executeBatch();
        }
    }

    @Nonnull
    private String insertLinkSql() {
        return insertLinkSql != null ? insertLinkSql : "INSERT INTO " + questPlayerTable + " (quest_id, player_id, abandoned) VALUES (?, ?, ?)";
    }

    /**
     * Binds the state, the moment it was reached, the document, the version and the time of the
     * write, as the first five parameters.
     */
    private static void bindRow(@Nonnull PreparedStatement statement, @Nonnull Snapshot snapshot, long version, long now) throws SQLException {
        statement.setString(1, snapshot.state().name());
        bindInstant(statement, 2, snapshot.completedAt());
        statement.setString(3, snapshot.data());
        statement.setLong(4, version);
        statement.setLong(5, now);
    }

    private static void bindInstant(@Nonnull PreparedStatement statement, int index, @Nullable Instant instant) throws SQLException {
        if (instant == null) {
            statement.setNull(index, Types.BIGINT);
        } else {
            statement.setLong(index, instant.toEpochMilli());
        }
    }

    @Nullable
    private Long readVersion(@Nonnull Connection connection, @Nonnull String questId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT version FROM " + questTable + " WHERE id = ?")) {
            statement.setString(1, questId);

            try (ResultSet results = statement.executeQuery()) {
                return results.next() ? results.getLong(1) : null;
            }
        }
    }

    /**
     * Reads quests from rows of id, document and version, each standing on the version it was
     * read at.
     */
    private static void readQuests(@Nonnull PreparedStatement statement, @Nonnull List<AbstractQuestProgression<?>> into) throws SQLException {
        try (ResultSet results = statement.executeQuery()) {
            while (results.next()) {
                AbstractQuestProgression<?> quest = readQuest(results.getString(2), results.getString(1));
                if (quest == null) continue;

                quest.markStored(AbstractQuestProgression.Stored.of(quest, results.getLong(3)));
                into.add(quest);
            }
        }
    }

    @Nonnull
    @Override
    public Set<UUID> loadIndex(@Nonnull String indexKey) {
        return loadIndexes(List.of(indexKey)).get(indexKey);
    }

    @Nonnull
    @Override
    public Map<String, Set<UUID>> loadIndexes(@Nonnull Collection<String> indexKeys) {
        Map<String, Set<UUID>> indexes = new HashMap<>();
        for (String indexKey : indexKeys) indexes.put(indexKey, new HashSet<>());
        if (indexKeys.isEmpty()) return indexes;

        List<String> keys = new ArrayList<>(indexKeys);

        return pool.with(connection -> {
            for (int from = 0; from < keys.size(); from += IN_CLAUSE_CHUNK) {
                List<String> chunk = keys.subList(from, Math.min(from + IN_CLAUSE_CHUNK, keys.size()));

                try (PreparedStatement statement = connection.prepareStatement("SELECT index_key, quest_id FROM " + questIndexTable + " WHERE index_key IN (" + placeholders(chunk.size()) + ")")) {
                    for (int i = 0; i < chunk.size(); i++) statement.setString(i + 1, chunk.get(i));

                    try (ResultSet results = statement.executeQuery()) {
                        while (results.next()) {
                            UUID questId = parseUuid(results.getString(2));
                            if (questId != null) indexes.get(results.getString(1)).add(questId);
                        }
                    }
                }
            }
            return indexes;
        });
    }

    @Override
    public void addToIndex(@Nonnull String indexKey, @Nonnull Collection<UUID> questIds) {
        if (questIds.isEmpty()) return;

        pool.inTransaction(connection -> {
            Collection<UUID> missing = insertMemberSql != null ? questIds : missingMembers(connection, indexKey, questIds);

            try (PreparedStatement insert = connection.prepareStatement(insertMemberSql != null ? insertMemberSql : "INSERT INTO " + questIndexTable + " (index_key, quest_id) VALUES (?, ?)")) {
                for (UUID questId : missing) {
                    insert.setString(1, indexKey);
                    insert.setString(2, questId.toString());
                    insert.addBatch();
                }
                insert.executeBatch();
            }
            return null;
        });
    }

    @Override
    public void removeFromIndex(@Nonnull String indexKey, @Nonnull Collection<UUID> questIds) {
        if (questIds.isEmpty()) return;

        pool.inTransaction(connection -> {
            try (PreparedStatement delete = connection.prepareStatement("DELETE FROM " + questIndexTable + " WHERE index_key = ? AND quest_id = ?")) {
                for (UUID questId : questIds) {
                    delete.setString(1, indexKey);
                    delete.setString(2, questId.toString());
                    delete.addBatch();
                }
                delete.executeBatch();
            }
            return null;
        });
    }

    @Override
    public void deleteIndex(@Nonnull String indexKey) {
        pool.with(connection -> {
            execute(connection, "DELETE FROM " + questIndexTable + " WHERE index_key = ?", indexKey);
            return null;
        });
    }

    @Nonnull
    @Override
    public AssignmentRecords loadAssignments(@Nonnull String holderKey) {
        return pool.with(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("SELECT assignment_id, quest_asset_id, handed_count, last_at, occasion FROM " + assignmentTable + " WHERE holder_key = ?")) {
                statement.setString(1, holderKey);

                AssignmentRecords records = new AssignmentRecords();
                try (ResultSet results = statement.executeQuery()) {
                    while (results.next()) {
                        records.put(results.getString(1), results.getString(2), new AssignmentRecord(results.getInt(3), results.getLong(4), results.getString(5)));
                    }
                }
                return records;
            }
        });
    }

    /**
     * The count doubles as the row's version: a server that read one count only writes over that
     * same count, and an insert of a row another server inserted first collides on the key.
     */
    @Override
    public boolean claimAssignment(@Nonnull String holderKey, @Nonnull String assignmentId, @Nonnull String questAssetId, @Nullable AssignmentRecord expected, @Nonnull AssignmentRecord next) {
        return pool.with(connection -> {
            if (expected == null) {
                try (PreparedStatement insert = connection.prepareStatement("INSERT INTO " + assignmentTable + " (holder_key, assignment_id, quest_asset_id, handed_count, last_at, occasion) VALUES (?, ?, ?, ?, ?, ?)")) {
                    insert.setString(1, holderKey);
                    insert.setString(2, assignmentId);
                    insert.setString(3, questAssetId);
                    insert.setInt(4, next.getCount());
                    insert.setLong(5, next.getLastAt().toEpochMilli());
                    insert.setString(6, next.getOccasion());
                    insert.executeUpdate();
                    return true;
                } catch (SQLException e) {
                    if (isDuplicateKey(e)) return false;
                    throw e;
                }
            }

            try (PreparedStatement update = connection.prepareStatement("UPDATE " + assignmentTable + " SET handed_count = ?, last_at = ?, occasion = ? WHERE holder_key = ? AND assignment_id = ? AND quest_asset_id = ? AND handed_count = ?")) {
                update.setInt(1, next.getCount());
                update.setLong(2, next.getLastAt().toEpochMilli());
                update.setString(3, next.getOccasion());
                update.setString(4, holderKey);
                update.setString(5, assignmentId);
                update.setString(6, questAssetId);
                update.setInt(7, expected.getCount());
                return update.executeUpdate() == 1;
            }
        });
    }

    @Override
    public void deleteAssignments(@Nonnull String holderKey) {
        pool.with(connection -> {
            execute(connection, "DELETE FROM " + assignmentTable + " WHERE holder_key = ?", holderKey);
            return null;
        });
    }

    /**
     * The quest ids come from the link table, not the player row: a quest another server handed
     * them is linked there and nowhere else.
     */
    @Nonnull
    @Override
    public PlayerQuestRecord loadPlayer(@Nonnull UUID playerId) {
        return pool.with(connection -> {
            PlayerQuestRecord record = readPlayer(connection, playerId);

            if (record == null) record = new PlayerQuestRecord();

            try (PreparedStatement statement = connection.prepareStatement("SELECT quest_id FROM " + questPlayerTable + " WHERE player_id = ?")) {
                statement.setString(1, playerId.toString());

                try (ResultSet results = statement.executeQuery()) {
                    while (results.next()) {
                        UUID questId = parseUuid(results.getString(1));
                        if (questId != null) record.getQuestIds().add(questId);
                    }
                }
            }
            return record;
        });
    }

    /**
     * Waits for the server holding the player to let go, which it does as it writes them out, and
     * takes them over once waiting is over: a server still holding them by then has stopped
     * answering, or will find its next write refused.
     */
    @Nonnull
    @Override
    public PlayerQuestRecord hostPlayer(@Nonnull UUID playerId) {
        long giveUpAt = System.currentTimeMillis() + HANDOFF_WAIT.toMillis();

        while (!takeHost(playerId, false)) {
            if (System.currentTimeMillis() >= giveUpAt) {
                LOGGER.atWarning().log("Player %s was still held by another server after %d s: taking them over", playerId, HANDOFF_WAIT.toSeconds());
                takeHost(playerId, true);
                break;
            }
            try {
                Thread.sleep(HANDOFF_POLL.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                takeHost(playerId, true);
                break;
            }
        }
        return loadPlayer(playerId);
    }

    /**
     * @param overrule takes the player whoever holds them
     * @return whether this server hosts them now
     */
    private boolean takeHost(@Nonnull UUID playerId, boolean overrule) {
        long now = System.currentTimeMillis();
        String free = overrule ? "" : " AND (host IS NULL OR host = ? OR host_until < ?)";

        return pool.with(connection -> {
            try (PreparedStatement update = connection.prepareStatement("UPDATE " + playerTable + " SET host = ?, host_until = ? WHERE player_id = ?" + free)) {
                update.setString(1, hostId);
                update.setLong(2, now + hostingMillis);
                update.setString(3, playerId.toString());
                if (!overrule) {
                    update.setString(4, hostId);
                    update.setLong(5, now);
                }
                if (update.executeUpdate() == 1) return true;
            }

            // Never hosted anywhere: their row starts here, unless another server started it first
            try (PreparedStatement insert = connection.prepareStatement("INSERT INTO " + playerTable + " (player_id, data, updated_at, host, host_until) VALUES (?, ?, ?, ?, ?)")) {
                insert.setString(1, playerId.toString());
                insert.setString(2, CodecJson.encode(PlayerQuestRecord.CODEC, new PlayerQuestRecord()));
                insert.setLong(3, now);
                insert.setString(4, hostId);
                insert.setLong(5, now + hostingMillis);
                insert.executeUpdate();
                return true;
            } catch (SQLException e) {
                if (isDuplicateKey(e)) return false;
                throw e;
            }
        });
    }

    /**
     * Written over only while no other server hosts the player. Hosting is left alone but for the
     * last write of a stay, which lets go of it.
     */
    @Override
    public boolean savePlayer(@Nonnull UUID playerId, @Nonnull PlayerQuestRecord record, @Nonnull Collection<UUID> delivered, boolean leaving) {
        // The link table owns the ids; a copy here could only disagree with it
        PlayerQuestRecord stored = new PlayerQuestRecord(Set.of(), record.getAssignments(), record.getPendingRewards(), record.getCompletions(), record.getTracking());
        String data = CodecJson.encode(PlayerQuestRecord.CODEC, stored);
        long now = System.currentTimeMillis();
        String letGo = leaving ? ", host = NULL, host_until = NULL" : "";

        boolean written = pool.inTransaction(connection -> {
            try (PreparedStatement update = connection.prepareStatement("UPDATE " + playerTable + " SET data = ?, updated_at = ?" + letGo + " WHERE player_id = ? AND (host IS NULL OR host = ?)")) {
                update.setString(1, data);
                update.setLong(2, now);
                update.setString(3, playerId.toString());
                update.setString(4, hostId);

                if (update.executeUpdate() == 0) {
                    if (readPlayer(connection, playerId) != null) return false;

                    try (PreparedStatement insert = connection.prepareStatement("INSERT INTO " + playerTable + " (player_id, data, updated_at) VALUES (?, ?, ?)")) {
                        insert.setString(1, playerId.toString());
                        insert.setString(2, data);
                        insert.setLong(3, now);
                        insert.executeUpdate();
                    }
                }
            }

            if (!delivered.isEmpty()) {
                try (PreparedStatement delete = connection.prepareStatement("DELETE FROM " + messageTable + " WHERE id = ?")) {
                    for (UUID messageId : delivered) {
                        delete.setString(1, messageId.toString());
                        delete.addBatch();
                    }
                    delete.executeBatch();
                }
            }
            return true;
        });

        if (!written) LOGGER.atWarning().log("Player %s is hosted by another server now: their record was not written here", playerId);
        return written;
    }

    @Override
    public void renewHosting(@Nonnull Duration validFor) {
        hostingMillis = validFor.toMillis();

        pool.with(connection -> {
            try (PreparedStatement update = connection.prepareStatement("UPDATE " + playerTable + " SET host_until = ? WHERE host = ?")) {
                update.setLong(1, System.currentTimeMillis() + hostingMillis);
                update.setString(2, hostId);
                update.executeUpdate();
            }
            return null;
        });
    }

    @Override
    public void postMessage(@Nonnull PlayerMessage message) {
        String data = CodecJson.encode(PlayerMessage.CODEC, message);

        pool.with(connection -> {
            try (PreparedStatement insert = connection.prepareStatement("INSERT INTO " + messageTable + " (id, player_id, data, created_at) VALUES (?, ?, ?, ?)")) {
                insert.setString(1, message.getId().toString());
                insert.setString(2, message.getPlayerId().toString());
                insert.setString(3, data);
                insert.setLong(4, System.currentTimeMillis());
                insert.executeUpdate();
            }
            return null;
        });
    }

    @Nonnull
    @Override
    public List<PlayerMessage> loadMessages(@Nonnull Collection<UUID> playerIds) {
        if (playerIds.isEmpty()) return List.of();

        List<UUID> ids = new ArrayList<>(playerIds);

        return pool.with(connection -> {
            List<PlayerMessage> messages = new ArrayList<>();

            for (int from = 0; from < ids.size(); from += IN_CLAUSE_CHUNK) {
                List<UUID> chunk = ids.subList(from, Math.min(from + IN_CLAUSE_CHUNK, ids.size()));

                try (PreparedStatement statement = connection.prepareStatement("SELECT id, data FROM " + messageTable + " WHERE player_id IN (" + placeholders(chunk.size()) + ") ORDER BY created_at")) {
                    bindIds(statement, 1, chunk);

                    try (ResultSet results = statement.executeQuery()) {
                        while (results.next()) {
                            PlayerMessage message = CodecJson.decode(PlayerMessage.CODEC, results.getString(2), "message " + results.getString(1));
                            if (message != null) messages.add(message);
                        }
                    }
                }
            }
            return messages;
        });
    }

    /**
     * Every database spells a key collision its own way: an integrity class state, the dedicated
     * exception, or SQLite's constraint code in the message.
     */
    private static boolean isDuplicateKey(@Nonnull SQLException e) {
        if (e instanceof SQLIntegrityConstraintViolationException) return true;

        String state = e.getSQLState();
        if (state != null && state.startsWith("23")) return true;

        String message = e.getMessage();
        return message != null && message.contains("SQLITE_CONSTRAINT");
    }

    @Nullable
    private PlayerQuestRecord readPlayer(@Nonnull Connection connection, @Nonnull UUID playerId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT data FROM " + playerTable + " WHERE player_id = ?")) {
            statement.setString(1, playerId.toString());

            try (ResultSet results = statement.executeQuery()) {
                if (!results.next()) return null;

                return CodecJson.decode(PlayerQuestRecord.CODEC, results.getString(1), "player " + playerId);
            }
        }
    }

    @Nonnull
    private Collection<UUID> missingMembers(@Nonnull Connection connection, @Nonnull String indexKey, @Nonnull Collection<UUID> questIds) throws SQLException {
        Set<UUID> missing = new HashSet<>(questIds);

        try (PreparedStatement statement = connection.prepareStatement("SELECT quest_id FROM " + questIndexTable + " WHERE index_key = ?")) {
            statement.setString(1, indexKey);

            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    UUID questId = parseUuid(results.getString(1));
                    if (questId != null) missing.remove(questId);
                }
            }
        }
        return missing;
    }

    private static void bindLink(@Nonnull PreparedStatement statement, @Nonnull String questId, @Nonnull UUID playerId, boolean abandoned) throws SQLException {
        statement.setString(1, questId);
        statement.setString(2, playerId.toString());
        statement.setBoolean(3, abandoned);
        statement.addBatch();
    }

    private static void bindIds(@Nonnull PreparedStatement statement, int first, @Nonnull List<UUID> ids) throws SQLException {
        for (int i = 0; i < ids.size(); i++) {
            statement.setString(first + i, ids.get(i).toString());
        }
    }

    @Nullable
    private static AbstractQuestProgression<?> readQuest(@Nullable String data, @Nonnull String questId) {
        if (data == null) return null;

        QuestProgressionRecord record = CodecJson.decode(QuestProgressionRecord.CODEC, data, "quest " + questId);
        return record == null ? null : record.quest;
    }

    private static void execute(@Nonnull Connection connection, @Nonnull String sql, @Nonnull String argument) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, argument);
            statement.executeUpdate();
        }
    }

    @Nonnull
    private static String placeholders(int count) {
        return "?, ".repeat(count - 1) + "?";
    }

    @Nullable
    private static UUID parseUuid(@Nullable String value) {
        if (value == null) return null;

        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            LOGGER.atWarning().log("Ignoring the malformed quest id %s", value);
            return null;
        }
    }

    /**
     * Checks the layout the tables were written in, and creates what is missing when allowed.
     * Tables of an older layout stop the server: read as this one, they would be misread.
     */
    private void prepareSchema() {
        pool.with(connection -> {
            Integer version = readSchemaVersion(connection);

            if (version == null && tableExists(connection, questTable)) {
                throw new QuestStorageException("The tables under the prefix '" + settings.tablePrefix() + "' were written by OpenQuests 3, and 4.0 lays quests out anew. Drop them or name another TablePrefix.");
            }
            if (version != null && version != SCHEMA_VERSION) {
                throw new QuestStorageException("The tables under the prefix '" + settings.tablePrefix() + "' are at layout " + version + ", this build reads layout " + SCHEMA_VERSION + ".");
            }

            if (settings.createSchema()) createSchema(connection);

            if (version == null && settings.createSchema()) {
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate("INSERT INTO " + versionTable + " (version) VALUES (" + SCHEMA_VERSION + ")");
                }
            }
            return null;
        });
    }

    @Nullable
    private Integer readSchemaVersion(@Nonnull Connection connection) {
        try (Statement statement = connection.createStatement();
             ResultSet results = statement.executeQuery("SELECT version FROM " + versionTable)) {
            return results.next() ? results.getInt(1) : null;
        } catch (SQLException e) {
            return null;
        }
    }

    private static boolean tableExists(@Nonnull Connection connection, @Nonnull String table) {
        try (Statement statement = connection.createStatement();
             ResultSet ignored = statement.executeQuery("SELECT 1 FROM " + table + " WHERE 1 = 0")) {
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Creates what is not there. Indexes go one by one and a failure is shrugged off: not every
     * database takes {@code IF NOT EXISTS} on one.
     */
    private void createSchema(@Nonnull Connection connection) throws SQLException {
        String text = dialect.getTextType();

        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS " + versionTable + " ("
                + "version INTEGER NOT NULL)");

            statement.execute("CREATE TABLE IF NOT EXISTS " + questTable + " ("
                + "id VARCHAR(36) NOT NULL PRIMARY KEY, "
                + "asset_id VARCHAR(255), "
                + "state VARCHAR(32) NOT NULL, "
                + "completed_at BIGINT, "
                + "data " + text + " NOT NULL, "
                + "version BIGINT NOT NULL, "
                + "updated_at BIGINT NOT NULL, "
                + "created_at BIGINT NOT NULL)");

            statement.execute("CREATE TABLE IF NOT EXISTS " + questPlayerTable + " ("
                + "quest_id VARCHAR(36) NOT NULL, "
                + "player_id VARCHAR(36) NOT NULL, "
                + "abandoned BOOLEAN NOT NULL, "
                + "PRIMARY KEY (quest_id, player_id))");

            statement.execute("CREATE TABLE IF NOT EXISTS " + questIndexTable + " ("
                + "index_key VARCHAR(190) NOT NULL, "
                + "quest_id VARCHAR(36) NOT NULL, "
                + "PRIMARY KEY (index_key, quest_id))");

            statement.execute("CREATE TABLE IF NOT EXISTS " + playerTable + " ("
                + "player_id VARCHAR(36) NOT NULL PRIMARY KEY, "
                + "data " + text + " NOT NULL, "
                + "updated_at BIGINT NOT NULL, "
                + "host VARCHAR(36), "
                + "host_until BIGINT)");

            statement.execute("CREATE TABLE IF NOT EXISTS " + messageTable + " ("
                + "id VARCHAR(36) NOT NULL PRIMARY KEY, "
                + "player_id VARCHAR(36) NOT NULL, "
                + "data " + text + " NOT NULL, "
                + "created_at BIGINT NOT NULL)");

            statement.execute("CREATE TABLE IF NOT EXISTS " + assignmentTable + " ("
                + "holder_key VARCHAR(190) NOT NULL, "
                + "assignment_id VARCHAR(190) NOT NULL, "
                + "quest_asset_id VARCHAR(190) NOT NULL, "
                + "handed_count INTEGER NOT NULL, "
                + "last_at BIGINT NOT NULL, "
                + "occasion VARCHAR(255) NOT NULL, "
                + "PRIMARY KEY (holder_key, assignment_id, quest_asset_id))");
        }

        createIndex(connection, "idx_" + questPlayerTable + "_player", questPlayerTable, "player_id");
        createIndex(connection, "idx_" + questIndexTable + "_quest", questIndexTable, "quest_id");
        createIndex(connection, "idx_" + questTable + "_asset", questTable, "asset_id");
        createIndex(connection, "idx_" + messageTable + "_player", messageTable, "player_id");
    }

    private static void createIndex(@Nonnull Connection connection, @Nonnull String name, @Nonnull String table, @Nonnull String column) {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE INDEX " + name + " ON " + table + " (" + column + ")");
        } catch (SQLException e) {
            LOGGER.atFine().log("Index %s was already there: %s", name, e.getMessage());
        }
    }

    /**
     * A quest as it is written, taken in one go while nothing changes it.
     *
     * @param before the stored copy it is written over
     */
    private record Snapshot(@Nonnull AbstractQuestProgression<?> quest, @Nonnull String id, @Nullable String assetId, @Nonnull String data,
                            @Nonnull QuestState state, @Nullable Instant completedAt,
                            @Nonnull Set<UUID> players, @Nonnull Set<UUID> abandoned, @Nonnull Stored before) {

        @Nonnull
        static Snapshot of(@Nonnull AbstractQuestProgression<?> quest) {
            synchronized (quest) {
                return new Snapshot(quest, quest.getId().toString(), quest.getAssetId(),
                    CodecJson.encode(QuestProgressionRecord.CODEC, new QuestProgressionRecord(quest)),
                    quest.getState(), quest.getCompletedAt(),
                    Set.copyOf(quest.getPlayers()), Set.copyOf(quest.getAbandonedPlayers()), quest.getStored());
            }
        }

        /**
         * @return the version the copy stands on, {@code 0} for one never written.
         */
        long version() {
            return before.version();
        }

        /**
         * @return what the quest stands on once this is written.
         */
        @Nonnull
        Stored written() {
            return new Stored(version() + 1, state, players, abandoned);
        }

        /**
         * @return the players whose link changes with this write.
         */
        @Nonnull
        Set<UUID> movedHolders() {
            Set<UUID> moved = new HashSet<>();
            for (UUID playerId : before.players()) if (!players.contains(playerId)) moved.add(playerId);
            for (UUID playerId : before.abandoned()) if (!abandoned.contains(playerId)) moved.add(playerId);
            for (UUID playerId : players) if (!before.players().contains(playerId)) moved.add(playerId);
            for (UUID playerId : abandoned) if (!before.abandoned().contains(playerId)) moved.add(playerId);
            return moved;
        }
    }

    /**
     * What the config said, once it has been read and checked.
     */
    public record JdbcSettings(@Nonnull String url,
                               @Nullable String user,
                               @Nullable String password,
                               @Nullable String driverClass,
                               @Nullable String driverPath,
                               @Nonnull String tablePrefix,
                               int poolSize,
                               int connectionTimeoutSeconds,
                               boolean createSchema,
                               @Nonnull SqlDialect dialect) {

        /**
         * The prefix goes into SQL unquoted, since a table name cannot be a parameter. Anything a
         * plain identifier cannot hold is refused rather than escaped.
         */
        public JdbcSettings {
            if (!tablePrefix.matches("[A-Za-z0-9_]*")) {
                throw new QuestStorageException("The quest storage table prefix may only hold letters, digits and underscores: " + tablePrefix);
            }
        }
    }
}
