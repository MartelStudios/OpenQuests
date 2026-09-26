package com.martelstudios.openquests.core.persistence.jdbc;

import com.hypixel.hytale.logger.HytaleLogger;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.persistence.CodecJson;
import com.martelstudios.openquests.core.persistence.PlayerQuestRecord;
import com.martelstudios.openquests.core.persistence.QuestProgressionRecord;
import com.martelstudios.openquests.core.persistence.QuestStorage;
import com.martelstudios.openquests.core.persistence.QuestStorageException;
import com.martelstudios.openquests.core.rewards.models.PendingRewards;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

/**
 * The quests in a relational database, for a server keeping tens of thousands of them or for
 * several servers sharing them.
 *
 * <p>A quest is one row carrying its document, its asset and state lifted out beside it so an
 * operator can query without reading JSON. Who holds a quest is a table of its own, which makes
 * "the quests of this player" an index lookup rather than a scan.
 *
 * <p><b>Two servers on one database</b> hand a player over cleanly. A quest several servers hold
 * <em>at once</em> is another matter: each keeps its own copy in memory and the last save wins,
 * with {@code updated_by} naming it.
 */
public class JdbcQuestStorage implements QuestStorage {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final String ID = "Jdbc";

    /**
     * How many ids go into one {@code IN (…)} list, past which another round trip is cheaper.
     */
    private static final int IN_CLAUSE_CHUNK = 500;

    private final JdbcSettings settings;
    private final SqlDialect dialect;

    private JdbcDriverLoader driverLoader;
    private JdbcConnectionPool pool;

    private String questTable;
    private String questPlayerTable;
    private String questIndexTable;
    private String playerTable;

    private String upsertQuestSql;
    private String upsertPlayerSql;

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

        upsertQuestSql = buildUpsert(questTable, "id", "id, asset_id, state, data, updated_at, updated_by", "asset_id = ?, state = ?, data = ?, updated_at = ?, updated_by = ?");
        upsertPlayerSql = buildUpsert(playerTable, "player_id", "player_id, data, updated_at", "data = ?, updated_at = ?");

        driverLoader = JdbcDriverLoader.load(settings.url(), settings.driverPath(), settings.driverClass());

        Properties properties = new Properties();
        if (settings.user() != null) properties.setProperty("user", settings.user());
        if (settings.password() != null) properties.setProperty("password", settings.password());

        pool = new JdbcConnectionPool(driverLoader.getDriver(), settings.url(), properties, settings.poolSize(), settings.connectionTimeoutSeconds());

        if (settings.createSchema()) createSchema();

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

    @Nullable
    @Override
    public AbstractQuestProgression<?> loadProgression(@Nonnull UUID questId) {
        return pool.with(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("SELECT data FROM " + questTable + " WHERE id = ?")) {
                statement.setString(1, questId.toString());

                try (ResultSet results = statement.executeQuery()) {
                    return results.next() ? readQuest(results.getString(1), questId.toString()) : null;
                }
            }
        });
    }

    @Nonnull
    @Override
    public List<AbstractQuestProgression<?>> loadProgressions(@Nonnull Collection<UUID> questIds) {
        if (questIds.isEmpty()) return List.of();

        List<UUID> ids = new ArrayList<>(questIds);
        List<AbstractQuestProgression<?>> quests = new ArrayList<>(ids.size());

        return pool.with(connection -> {
            for (int from = 0; from < ids.size(); from += IN_CLAUSE_CHUNK) {
                List<UUID> chunk = ids.subList(from, Math.min(from + IN_CLAUSE_CHUNK, ids.size()));

                try (PreparedStatement statement = connection.prepareStatement("SELECT id, data FROM " + questTable + " WHERE id IN (" + placeholders(chunk.size()) + ")")) {
                    for (int i = 0; i < chunk.size(); i++) {
                        statement.setString(i + 1, chunk.get(i).toString());
                    }

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
            String sql = "SELECT q.id, q.data FROM " + questTable + " q"
                + " JOIN " + questPlayerTable + " l ON l.quest_id = q.id"
                + " WHERE l.player_id = ?";

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, playerId.toString());

                List<AbstractQuestProgression<?>> quests = new ArrayList<>();
                readQuests(statement, quests);
                return quests;
            }
        });
    }

    @Nonnull
    @Override
    public List<AbstractQuestProgression<?>> loadAllProgressions() {
        return pool.with(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("SELECT id, data FROM " + questTable)) {
                List<AbstractQuestProgression<?>> quests = new ArrayList<>();
                readQuests(statement, quests);
                return quests;
            }
        });
    }

    /**
     * One transaction and four batches, however many quests there are.
     */
    @Override
    public void saveProgressions(@Nonnull Collection<AbstractQuestProgression<?>> quests) {
        if (quests.isEmpty()) return;

        long now = System.currentTimeMillis();

        pool.inTransaction(connection -> {
            try (PreparedStatement deleteQuest = dialect.supportsUpsert() ? null : connection.prepareStatement("DELETE FROM " + questTable + " WHERE id = ?");
                 PreparedStatement upsertQuest = connection.prepareStatement(upsertQuestSql);
                 PreparedStatement deleteLinks = connection.prepareStatement("DELETE FROM " + questPlayerTable + " WHERE quest_id = ?");
                 PreparedStatement insertLink = connection.prepareStatement("INSERT INTO " + questPlayerTable + " (quest_id, player_id, abandoned) VALUES (?, ?, ?)")) {

                for (AbstractQuestProgression<?> quest : quests) {
                    String questId = quest.getId().toString();
                    String data = CodecJson.encode(QuestProgressionRecord.CODEC, new QuestProgressionRecord(quest));

                    if (deleteQuest != null) {
                        deleteQuest.setString(1, questId);
                        deleteQuest.addBatch();
                    }

                    bindQuest(upsertQuest, questId, quest, data, now);
                    upsertQuest.addBatch();

                    deleteLinks.setString(1, questId);
                    deleteLinks.addBatch();

                    for (UUID playerId : quest.getPlayers()) {
                        bindLink(insertLink, questId, playerId, false);
                    }

                    for (UUID playerId : quest.getAbandonedPlayers()) {
                        // Disjoint by construction; a record saying otherwise would fail the batch
                        if (quest.getPlayers().contains(playerId)) continue;

                        bindLink(insertLink, questId, playerId, true);
                    }
                }

                // Deletes first: a link still standing is a row the insert would collide with
                if (deleteQuest != null) deleteQuest.executeBatch();
                deleteLinks.executeBatch();
                upsertQuest.executeBatch();
                insertLink.executeBatch();
            }
            return null;
        });
    }

    @Override
    public void deleteProgression(@Nonnull AbstractQuestProgression<?> quest) {
        String questId = quest.getId().toString();

        pool.inTransaction(connection -> {
            execute(connection, "DELETE FROM " + questPlayerTable + " WHERE quest_id = ?", questId);
            execute(connection, "DELETE FROM " + questIndexTable + " WHERE quest_id = ?", questId);
            execute(connection, "DELETE FROM " + questTable + " WHERE id = ?", questId);
            return null;
        });
    }

    @Nonnull
    @Override
    public Set<UUID> loadIndex(@Nonnull String indexKey) {
        return pool.with(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("SELECT quest_id FROM " + questIndexTable + " WHERE index_key = ?")) {
                statement.setString(1, indexKey);

                Set<UUID> questIds = new HashSet<>();
                try (ResultSet results = statement.executeQuery()) {
                    while (results.next()) {
                        UUID questId = parseUuid(results.getString(1));
                        if (questId != null) questIds.add(questId);
                    }
                }
                return questIds;
            }
        });
    }

    @Override
    public void saveIndex(@Nonnull String indexKey, @Nonnull Set<UUID> questIds) {
        pool.inTransaction(connection -> {
            execute(connection, "DELETE FROM " + questIndexTable + " WHERE index_key = ?", indexKey);

            if (questIds.isEmpty()) return null;

            try (PreparedStatement insert = connection.prepareStatement("INSERT INTO " + questIndexTable + " (index_key, quest_id) VALUES (?, ?)")) {
                for (UUID questId : questIds) {
                    insert.setString(1, indexKey);
                    insert.setString(2, questId.toString());
                    insert.addBatch();
                }
                insert.executeBatch();
            }
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
    public void savePlayer(@Nonnull UUID playerId, @Nonnull PlayerQuestRecord record) {
        // The link table owns the ids; a copy here could only disagree with it
        PlayerQuestRecord stored = new PlayerQuestRecord(Set.of(), record.getStartedOnConnection(), record.getPendingRewards(), record.getCompletions());

        pool.inTransaction(connection -> {
            writePlayer(connection, playerId, stored);
            return null;
        });
    }

    @Override
    public void addPendingRewards(@Nonnull UUID playerId, @Nonnull PendingRewards owed) {
        pool.inTransaction(connection -> {
            PlayerQuestRecord record = readPlayer(connection, playerId);
            if (record == null) record = new PlayerQuestRecord();

            record.getPendingRewards().remove(owed);
            record.getPendingRewards().add(owed);

            writePlayer(connection, playerId, record);
            return null;
        });
    }

    @Override
    public void recordCompletion(@Nonnull UUID playerId, @Nonnull String assetId, @Nonnull QuestState outcome, @Nullable Instant startedAt, @Nullable Instant completedAt) {
        pool.inTransaction(connection -> {
            PlayerQuestRecord record = readPlayer(connection, playerId);
            if (record == null) record = new PlayerQuestRecord();

            record.recordCompletion(assetId, outcome, startedAt, completedAt);

            writePlayer(connection, playerId, record);
            return null;
        });
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

    private void writePlayer(@Nonnull Connection connection, @Nonnull UUID playerId, @Nonnull PlayerQuestRecord record) throws SQLException {
        String data = CodecJson.encode(PlayerQuestRecord.CODEC, record);
        long now = System.currentTimeMillis();

        if (!dialect.supportsUpsert()) {
            execute(connection, "DELETE FROM " + playerTable + " WHERE player_id = ?", playerId.toString());
        }

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
    }

    private void bindQuest(@Nonnull PreparedStatement statement, @Nonnull String questId, @Nonnull AbstractQuestProgression<?> quest, @Nonnull String data, long now) throws SQLException {
        QuestState state = quest.getState();

        int index = 1;
        statement.setString(index++, questId);
        statement.setString(index++, quest.getAssetId());
        statement.setString(index++, state == null ? null : state.name());
        statement.setString(index++, data);
        statement.setLong(index++, now);
        statement.setString(index++, settings.serverId());

        if (!dialect.supportsUpsert()) return;

        statement.setString(index++, quest.getAssetId());
        statement.setString(index++, state == null ? null : state.name());
        statement.setString(index++, data);
        statement.setLong(index++, now);
        statement.setString(index, settings.serverId());
    }

    private static void bindLink(@Nonnull PreparedStatement statement, @Nonnull String questId, @Nonnull UUID playerId, boolean abandoned) throws SQLException {
        statement.setString(1, questId);
        statement.setString(2, playerId.toString());
        statement.setBoolean(3, abandoned);
        statement.addBatch();
    }

    private void readQuests(@Nonnull PreparedStatement statement, @Nonnull List<AbstractQuestProgression<?>> into) throws SQLException {
        try (ResultSet results = statement.executeQuery()) {
            while (results.next()) {
                AbstractQuestProgression<?> quest = readQuest(results.getString(2), results.getString(1));
                if (quest != null) into.add(quest);
            }
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
     * Creates what is not there. Indexes go one by one and a failure is shrugged off: not every
     * database takes {@code IF NOT EXISTS} on one.
     */
    private void createSchema() {
        String text = dialect.getTextType();

        pool.with(connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE IF NOT EXISTS " + questTable + " ("
                    + "id VARCHAR(36) NOT NULL PRIMARY KEY, "
                    + "asset_id VARCHAR(255), "
                    + "state VARCHAR(32), "
                    + "data " + text + " NOT NULL, "
                    + "updated_at BIGINT NOT NULL, "
                    + "updated_by VARCHAR(64))");

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
            }

            createIndex(connection, "idx_" + questPlayerTable + "_player", questPlayerTable, "player_id");
            createIndex(connection, "idx_" + questIndexTable + "_quest", questIndexTable, "quest_id");
            createIndex(connection, "idx_" + questTable + "_asset", questTable, "asset_id");
            return null;
        });
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
     * @param serverId names this server in {@code updated_by}, which is the only way to tell who
     * wrote a row when several share the database.
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
