package com.martelstudios.openquests.core.persistence.jdbc;

import com.hypixel.hytale.logger.HytaleLogger;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.AssignmentRecord;
import com.martelstudios.openquests.core.models.AssignmentRecords;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.persistence.CodecJson;
import com.martelstudios.openquests.core.persistence.PlayerMessage;
import com.martelstudios.openquests.core.persistence.PlayerQuestRecord;
import com.martelstudios.openquests.core.persistence.QuestProgressionRecord;
import com.martelstudios.openquests.core.persistence.QuestReplica;
import com.martelstudios.openquests.core.persistence.QuestStorage;
import com.martelstudios.openquests.core.persistence.QuestStorageException;
import com.martelstudios.openquests.core.persistence.ReplicaPoll;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The quests in a relational database, for a server keeping tens of thousands of them or for
 * several servers sharing them.
 *
 * <p>A quest is a row of its own, carrying its asset and the outcome claimed for it, and one
 * replica row per server that ever wrote it, holding that server's document. A server only ever
 * writes its own replica, so servers sharing a quest never overwrite one another; reading a quest
 * merges its replicas. Who holds a quest is a table of its own, which makes "the quests of this
 * player" an index lookup rather than a scan.
 */
public class JdbcQuestStorage implements QuestStorage {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final String ID = "Jdbc";

    /**
     * The layout this version writes. Tables written by another are refused rather than misread.
     */
    private static final int SCHEMA_VERSION = 4;

    /**
     * How many ids go into one {@code IN (…)} list, past which another round trip is cheaper.
     */
    private static final int IN_CLAUSE_CHUNK = 500;

    private static final String RUNNING = QuestState.IN_PROGRESS.name();

    private final JdbcSettings settings;
    private final SqlDialect dialect;

    /**
     * The revision last written of each replica of this server, so the next one is greater even
     * when two writes fall in the same millisecond.
     */
    private final Map<UUID, Long> revisions = new ConcurrentHashMap<>();

    private JdbcDriverLoader driverLoader;
    private JdbcConnectionPool pool;

    private String questTable;
    private String replicaTable;
    private String questPlayerTable;
    private String questIndexTable;
    private String playerTable;
    private String messageTable;
    private String assignmentTable;
    private String versionTable;

    private String upsertReplicaSql;
    private String upsertPlayerSql;

    @Nullable
    private String insertQuestSql;

    @Nullable
    private String insertMemberSql;

    @Nullable
    private String insertLinkSql;

    public JdbcQuestStorage(@Nonnull JdbcSettings settings) {
        this.settings = settings;
        this.dialect = settings.dialect();
    }

    @Override
    public void start() {
        String prefix = settings.tablePrefix();

        questTable = prefix + "quest";
        replicaTable = prefix + "quest_replica";
        questPlayerTable = prefix + "quest_player";
        questIndexTable = prefix + "quest_index";
        playerTable = prefix + "player";
        messageTable = prefix + "player_message";
        assignmentTable = prefix + "quest_assignment";
        versionTable = prefix + "schema_version";

        upsertReplicaSql = buildUpsert(replicaTable, "quest_id, server_id", "quest_id, server_id, revision, data, updated_at", "revision = ?, data = ?, updated_at = ?");
        upsertPlayerSql = buildUpsert(playerTable, "player_id", "player_id, data, updated_at", "data = ?, updated_at = ?");
        insertQuestSql = dialect.insertIgnoring(questTable, "id, asset_id, state, created_at");
        insertMemberSql = dialect.insertIgnoring(questIndexTable, "index_key, quest_id");
        insertLinkSql = dialect.insertIgnoring(questPlayerTable, "quest_id, player_id, abandoned");

        driverLoader = JdbcDriverLoader.load(settings.url(), settings.driverPath(), settings.driverClass());

        Properties properties = new Properties();
        if (settings.user() != null) properties.setProperty("user", settings.user());
        if (settings.password() != null) properties.setProperty("password", settings.password());

        pool = new JdbcConnectionPool(driverLoader.getDriver(), settings.url(), properties, settings.poolSize(), settings.connectionTimeoutSeconds());

        prepareSchema();

        LOGGER.atInfo().log("Quest storage: %s as %s, server id %s", settings.url(), dialect, settings.serverId());
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

    @Nonnull
    @Override
    public String getReplicaId() {
        return settings.serverId();
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
        Map<String, AbstractQuestProgression<?>> merged = new LinkedHashMap<>();

        return pool.with(connection -> {
            for (int from = 0; from < ids.size(); from += IN_CLAUSE_CHUNK) {
                List<UUID> chunk = ids.subList(from, Math.min(from + IN_CLAUSE_CHUNK, ids.size()));

                try (PreparedStatement statement = connection.prepareStatement(selectMerged() + " WHERE r.quest_id IN (" + placeholders(chunk.size()) + ")")) {
                    bindIds(statement, 1, chunk);
                    readMerged(statement, merged);
                }
            }
            return new ArrayList<>(merged.values());
        });
    }

    @Nonnull
    @Override
    public List<AbstractQuestProgression<?>> loadPlayerProgressions(@Nonnull UUID playerId) {
        return pool.with(connection -> {
            String sql = selectMerged() + " JOIN " + questPlayerTable + " l ON l.quest_id = r.quest_id WHERE l.player_id = ?";

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, playerId.toString());

                Map<String, AbstractQuestProgression<?>> merged = new LinkedHashMap<>();
                readMerged(statement, merged);

                List<AbstractQuestProgression<?>> held = new ArrayList<>();
                for (AbstractQuestProgression<?> quest : merged.values()) {
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
            try (PreparedStatement statement = connection.prepareStatement(selectMerged())) {
                Map<String, AbstractQuestProgression<?>> merged = new LinkedHashMap<>();
                readMerged(statement, merged);
                return new ArrayList<>(merged.values());
            }
        });
    }

    /**
     * One transaction, whatever the number of quests: their rows where missing, this server's
     * replicas, and who holds each.
     */
    @Override
    public void saveProgressions(@Nonnull Collection<AbstractQuestProgression<?>> quests) {
        if (quests.isEmpty()) return;

        long now = System.currentTimeMillis();

        pool.inTransaction(connection -> {
            insertQuestRows(connection, quests, now);

            try (PreparedStatement deleteReplica = dialect.supportsUpsert() ? null : connection.prepareStatement("DELETE FROM " + replicaTable + " WHERE quest_id = ? AND server_id = ?");
                 PreparedStatement upsertReplica = connection.prepareStatement(upsertReplicaSql);
                 PreparedStatement deleteLinks = connection.prepareStatement("DELETE FROM " + questPlayerTable + " WHERE quest_id = ?");
                 PreparedStatement insertLink = connection.prepareStatement(insertLinkSql != null ? insertLinkSql : "INSERT INTO " + questPlayerTable + " (quest_id, player_id, abandoned) VALUES (?, ?, ?)")) {

                for (AbstractQuestProgression<?> quest : quests) {
                    String questId = quest.getId().toString();
                    String data = CodecJson.encode(QuestProgressionRecord.CODEC, new QuestProgressionRecord(quest));
                    long revision = revisions.merge(quest.getId(), now, (last, next) -> Math.max(last + 1, next));

                    if (deleteReplica != null) {
                        deleteReplica.setString(1, questId);
                        deleteReplica.setString(2, settings.serverId());
                        deleteReplica.addBatch();
                    }

                    bindReplica(upsertReplica, questId, revision, data, now);
                    upsertReplica.addBatch();

                    deleteLinks.setString(1, questId);
                    deleteLinks.addBatch();

                    for (UUID playerId : quest.getPlayers()) {
                        bindLink(insertLink, questId, playerId, false);
                    }
                    for (UUID playerId : quest.getAbandonedPlayers()) {
                        bindLink(insertLink, questId, playerId, true);
                    }
                }

                // Deletes first: a row still standing is one the insert would collide with
                if (deleteReplica != null) deleteReplica.executeBatch();
                deleteLinks.executeBatch();
                upsertReplica.executeBatch();
                insertLink.executeBatch();
            }
            return null;
        });
    }

    @Override
    public void deleteProgression(@Nonnull AbstractQuestProgression<?> quest) {
        String questId = quest.getId().toString();
        revisions.remove(quest.getId());

        pool.inTransaction(connection -> {
            execute(connection, "DELETE FROM " + questPlayerTable + " WHERE quest_id = ?", questId);
            execute(connection, "DELETE FROM " + questIndexTable + " WHERE quest_id = ?", questId);
            execute(connection, "DELETE FROM " + replicaTable + " WHERE quest_id = ?", questId);
            execute(connection, "DELETE FROM " + questTable + " WHERE id = ?", questId);
            return null;
        });
    }

    /**
     * The row is made sure of first, so the claim is one conditional update whichever server
     * wrote the quest first: only the update finding it still running writes.
     */
    @Override
    public boolean claimEnd(@Nonnull AbstractQuestProgression<?> quest) {
        long now = System.currentTimeMillis();
        Instant completedAt = quest.getCompletedAt();

        return pool.inTransaction(connection -> {
            insertQuestRows(connection, List.of(quest), now);

            try (PreparedStatement update = connection.prepareStatement("UPDATE " + questTable + " SET state = ?, completed_at = ? WHERE id = ? AND state = ?")) {
                update.setString(1, quest.getState().name());
                update.setLong(2, completedAt == null ? now : completedAt.toEpochMilli());
                update.setString(3, quest.getId().toString());
                update.setString(4, RUNNING);
                return update.executeUpdate() == 1;
            }
        });
    }

    @Nullable
    @Override
    public QuestState loadEnd(@Nonnull UUID questId) {
        return pool.with(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("SELECT state FROM " + questTable + " WHERE id = ?")) {
                statement.setString(1, questId.toString());

                try (ResultSet results = statement.executeQuery()) {
                    return results.next() ? ended(results.getString(1)) : null;
                }
            }
        });
    }

    /**
     * Two reads: the revisions of every other server's replica of those quests, then the
     * documents of the replicas that moved, the only ones worth carrying over the wire.
     */
    @Nonnull
    @Override
    public ReplicaPoll pollReplicas(@Nonnull Map<UUID, Map<String, Long>> known) {
        if (known.isEmpty()) return ReplicaPoll.NONE;

        List<UUID> ids = new ArrayList<>(known.keySet());

        return pool.with(connection -> {
            List<QuestReplica> changed = new ArrayList<>();
            Map<UUID, QuestState> ended = new HashMap<>();

            for (int from = 0; from < ids.size(); from += IN_CLAUSE_CHUNK) {
                List<UUID> chunk = ids.subList(from, Math.min(from + IN_CLAUSE_CHUNK, ids.size()));

                List<String[]> moved = new ArrayList<>();
                try (PreparedStatement statement = connection.prepareStatement("SELECT quest_id, server_id, revision FROM " + replicaTable + " WHERE server_id <> ? AND quest_id IN (" + placeholders(chunk.size()) + ")")) {
                    statement.setString(1, settings.serverId());
                    bindIds(statement, 2, chunk);

                    try (ResultSet results = statement.executeQuery()) {
                        while (results.next()) {
                            UUID questId = parseUuid(results.getString(1));
                            if (questId == null) continue;

                            long seen = known.getOrDefault(questId, Map.of()).getOrDefault(results.getString(2), -1L);
                            if (results.getLong(3) > seen) moved.add(new String[]{results.getString(1), results.getString(2)});
                        }
                    }
                }

                if (!moved.isEmpty()) readReplicas(connection, moved, changed);

                try (PreparedStatement statement = connection.prepareStatement("SELECT id, state FROM " + questTable + " WHERE state <> ? AND id IN (" + placeholders(chunk.size()) + ")")) {
                    statement.setString(1, RUNNING);
                    bindIds(statement, 2, chunk);

                    try (ResultSet results = statement.executeQuery()) {
                        while (results.next()) {
                            UUID questId = parseUuid(results.getString(1));
                            QuestState outcome = ended(results.getString(2));
                            if (questId != null && outcome != null) ended.put(questId, outcome);
                        }
                    }
                }
            }
            return new ReplicaPoll(changed, ended);
        });
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

    @Override
    public void savePlayer(@Nonnull UUID playerId, @Nonnull PlayerQuestRecord record, @Nonnull Collection<UUID> delivered) {
        // The link table owns the ids; a copy here could only disagree with it
        PlayerQuestRecord stored = new PlayerQuestRecord(Set.of(), record.getAssignments(), record.getPendingRewards(), record.getCompletions());
        String data = CodecJson.encode(PlayerQuestRecord.CODEC, stored);
        long now = System.currentTimeMillis();

        pool.inTransaction(connection -> {
            if (!dialect.supportsUpsert()) execute(connection, "DELETE FROM " + playerTable + " WHERE player_id = ?", playerId.toString());

            try (PreparedStatement statement = connection.prepareStatement(upsertPlayerSql)) {
                int index = 1;
                statement.setString(index++, playerId.toString());
                statement.setString(index++, data);
                statement.setLong(index++, now);

                if (dialect.supportsUpsert()) {
                    statement.setString(index++, data);
                    statement.setLong(index, now);
                }
                statement.executeUpdate();
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

    /**
     * Writes the row of each quest that has none yet, running: its outcome is only ever written
     * by a claim.
     */
    private void insertQuestRows(@Nonnull Connection connection, @Nonnull Collection<AbstractQuestProgression<?>> quests, long now) throws SQLException {
        List<AbstractQuestProgression<?>> missing = new ArrayList<>(quests);
        if (insertQuestSql == null) {
            Set<UUID> existing = existingQuests(connection, quests);
            missing.removeIf(quest -> existing.contains(quest.getId()));
        }

        try (PreparedStatement insert = connection.prepareStatement(insertQuestSql != null ? insertQuestSql : "INSERT INTO " + questTable + " (id, asset_id, state, created_at) VALUES (?, ?, ?, ?)")) {
            for (AbstractQuestProgression<?> quest : missing) {
                insert.setString(1, quest.getId().toString());
                insert.setString(2, quest.getAssetId());
                insert.setString(3, RUNNING);
                insert.setLong(4, now);
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    @Nonnull
    private Set<UUID> existingQuests(@Nonnull Connection connection, @Nonnull Collection<AbstractQuestProgression<?>> quests) throws SQLException {
        List<UUID> ids = quests.stream().map(AbstractQuestProgression::getId).toList();
        Set<UUID> existing = new HashSet<>();

        for (int from = 0; from < ids.size(); from += IN_CLAUSE_CHUNK) {
            List<UUID> chunk = ids.subList(from, Math.min(from + IN_CLAUSE_CHUNK, ids.size()));

            try (PreparedStatement statement = connection.prepareStatement("SELECT id FROM " + questTable + " WHERE id IN (" + placeholders(chunk.size()) + ")")) {
                bindIds(statement, 1, chunk);

                try (ResultSet results = statement.executeQuery()) {
                    while (results.next()) {
                        UUID questId = parseUuid(results.getString(1));
                        if (questId != null) existing.add(questId);
                    }
                }
            }
        }
        return existing;
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

    /**
     * Every replica of the quests asked for, with the outcome claimed for each, so one read builds
     * them whole.
     */
    @Nonnull
    private String selectMerged() {
        return "SELECT r.quest_id, r.data, q.state, q.completed_at FROM " + replicaTable + " r LEFT JOIN " + questTable + " q ON q.id = r.quest_id";
    }

    /**
     * Merges the replicas of each quest into the first read, then applies the outcome claimed for
     * it, which every replica agrees on whatever it saw.
     */
    private void readMerged(@Nonnull PreparedStatement statement, @Nonnull Map<String, AbstractQuestProgression<?>> into) throws SQLException {
        Map<String, QuestState> ends = new HashMap<>();
        Map<String, Long> endedAt = new HashMap<>();

        try (ResultSet results = statement.executeQuery()) {
            while (results.next()) {
                String questId = results.getString(1);
                AbstractQuestProgression<?> replica = readQuest(results.getString(2), questId);
                if (replica == null) continue;

                AbstractQuestProgression<?> first = into.putIfAbsent(questId, replica);
                if (first != null) first.merge(replica);

                QuestState outcome = ended(results.getString(3));
                if (outcome != null) {
                    ends.put(questId, outcome);
                    endedAt.put(questId, results.getLong(4));
                }
            }
        }

        ends.forEach((questId, outcome) -> {
            AbstractQuestProgression<?> quest = into.get(questId);
            if (quest != null) quest.settle(outcome, Instant.ofEpochMilli(endedAt.get(questId)));
        });
    }

    /**
     * The documents of the replicas that moved, and only those.
     *
     * @param moved each replica as its quest id and server id
     */
    private void readReplicas(@Nonnull Connection connection, @Nonnull List<String[]> moved, @Nonnull List<QuestReplica> into) throws SQLException {
        for (int from = 0; from < moved.size(); from += IN_CLAUSE_CHUNK) {
            List<String[]> chunk = moved.subList(from, Math.min(from + IN_CLAUSE_CHUNK, moved.size()));
            String keys = "(quest_id = ? AND server_id = ?)" + " OR (quest_id = ? AND server_id = ?)".repeat(chunk.size() - 1);

            try (PreparedStatement statement = connection.prepareStatement("SELECT quest_id, server_id, revision, data FROM " + replicaTable + " WHERE " + keys)) {
                for (int i = 0; i < chunk.size(); i++) {
                    statement.setString(2 * i + 1, chunk.get(i)[0]);
                    statement.setString(2 * i + 2, chunk.get(i)[1]);
                }

                try (ResultSet results = statement.executeQuery()) {
                    while (results.next()) {
                        UUID questId = parseUuid(results.getString(1));
                        AbstractQuestProgression<?> replica = readQuest(results.getString(4), results.getString(1));
                        if (questId != null && replica != null) into.add(new QuestReplica(questId, results.getString(2), results.getLong(3), replica));
                    }
                }
            }
        }
    }

    private void bindReplica(@Nonnull PreparedStatement statement, @Nonnull String questId, long revision, @Nonnull String data, long now) throws SQLException {
        int index = 1;
        statement.setString(index++, questId);
        statement.setString(index++, settings.serverId());
        statement.setLong(index++, revision);
        statement.setString(index++, data);
        statement.setLong(index++, now);

        if (!dialect.supportsUpsert()) return;

        statement.setLong(index++, revision);
        statement.setString(index++, data);
        statement.setLong(index, now);
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

    /**
     * @return the outcome a state column holds, {@code null} for a quest still running or none.
     */
    @Nullable
    private static QuestState ended(@Nullable String state) {
        if (state == null || RUNNING.equals(state)) return null;

        try {
            return QuestState.valueOf(state);
        } catch (IllegalArgumentException e) {
            LOGGER.atWarning().log("Ignoring the unknown quest state %s", state);
            return null;
        }
    }

    private static void execute(@Nonnull Connection connection, @Nonnull String sql, @Nonnull String argument) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, argument);
            statement.executeUpdate();
        }
    }

    /**
     * An upsert where the dialect has one, a plain insert where the caller deletes the row first.
     */
    @Nonnull
    private String buildUpsert(@Nonnull String table, @Nonnull String keyColumns, @Nonnull String columns, @Nonnull String assignments) {
        String insert = "INSERT INTO " + table + " (" + columns + ") VALUES (" + placeholders(columns.split(",").length) + ")";

        return dialect.supportsUpsert() ? insert + dialect.upsertClause(keyColumns) + assignments : insert;
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
                + "created_at BIGINT NOT NULL)");

            statement.execute("CREATE TABLE IF NOT EXISTS " + replicaTable + " ("
                + "quest_id VARCHAR(36) NOT NULL, "
                + "server_id VARCHAR(64) NOT NULL, "
                + "revision BIGINT NOT NULL, "
                + "data " + text + " NOT NULL, "
                + "updated_at BIGINT NOT NULL, "
                + "PRIMARY KEY (quest_id, server_id))");

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
                + "updated_at BIGINT NOT NULL)");

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
     * What the config said, once it has been read and checked.
     *
     * @param serverId names this server's replicas, so it must differ between servers sharing the
     * database: two under one name would overwrite each other's progress.
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
                               @Nonnull String serverId,
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
