package com.martelstudios.openquests.core.persistence;

import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.QuestCompletions;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.persistence.jdbc.JdbcQuestStorage;
import com.martelstudios.openquests.core.persistence.jdbc.SqlDialect;
import com.martelstudios.openquests.core.rewards.QuestReward;
import com.martelstudios.openquests.core.rewards.models.PendingRewards;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.time.Instant;
import java.util.stream.Collectors;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The JDBC backend against a real database. H2 covers the delete-then-insert path every run;
 * PostgreSQL covers {@code ON CONFLICT} when a URL is handed in, which is what
 * {@code docker/postgres.yml} is for.
 */
class JdbcQuestStorageTest {

    private static final String PROPERTY_URL = "openquests.jdbc.url";

    private QuestStorage storage;

    @BeforeAll
    static void registerTypes() {
        AbstractQuestProgression.CODEC.register(TestQuestProgression.TYPE, TestQuestProgression.class, TestQuestProgression.CODEC);
        QuestReward.CODEC.register(TestQuestReward.TYPE, TestQuestReward.class, TestQuestReward.CODEC);
    }

    @BeforeEach
    void openH2() {
        storage = open("jdbc:h2:mem:openquests-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", null, null);
    }

    @AfterEach
    void close() {
        if (storage != null) storage.close();
    }

    @Test
    void writesAndReadsBackAQuestWhole() {
        UUID playerId = UUID.randomUUID();
        TestQuestProgression quest = quest("CollectStick", playerId);
        quest.setCounter(7).setState(QuestState.IN_PROGRESS).addTag("OQ_TEST", "a", "b");

        storage.saveProgressions(List.of(quest));

        AbstractQuestProgression<?> read = storage.loadProgression(quest.getId());

        assertNotNull(read);
        assertEquals(quest.getId(), read.getId());
        assertEquals("CollectStick", read.getAssetId());
        assertEquals(QuestState.IN_PROGRESS, read.getState());
        assertEquals(Set.of(playerId), read.getPlayers());
        assertEquals(7, ((TestQuestProgression) read).getCounter());
        assertTrue(read.hasTag("OQ_TEST"));
        assertEquals(List.of("a", "b"), List.of(read.getTagValues("OQ_TEST")));
        assertNotNull(read.getStartedAt());
    }

    @Test
    void savingTheSameQuestTwiceUpdatesIt() {
        TestQuestProgression quest = quest("CollectStick", UUID.randomUUID());

        storage.saveProgressions(List.of(quest));

        quest.setCounter(42).setState(QuestState.SUCCESSFUL);
        storage.saveProgressions(List.of(quest));

        assertEquals(1, storage.loadAllProgressions().size());

        AbstractQuestProgression<?> read = storage.loadProgression(quest.getId());
        assertNotNull(read);
        assertEquals(QuestState.SUCCESSFUL, read.getState());
        assertEquals(42, ((TestQuestProgression) read).getCounter());
    }

    @Test
    void readsEveryQuestOfOnePlayerInOneGo() {
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();

        TestQuestProgression hers = quest("Hers", alice);
        TestQuestProgression his = quest("His", bob);
        TestQuestProgression shared = quest("Shared", alice, bob);

        storage.saveProgressions(List.of(hers, his, shared));

        assertEquals(Set.of(hers.getId(), shared.getId()), ids(storage.loadPlayerProgressions(alice)));
        assertEquals(Set.of(his.getId(), shared.getId()), ids(storage.loadPlayerProgressions(bob)));
    }

    @Test
    void aPlayerWhoGaveUpStillHoldsTheQuest() {
        UUID playerId = UUID.randomUUID();
        TestQuestProgression quest = quest("Abandoned", playerId);
        quest.getPlayers().remove(playerId);
        quest.getAbandonedPlayers().add(playerId);

        storage.saveProgressions(List.of(quest));

        assertEquals(Set.of(quest.getId()), ids(storage.loadPlayerProgressions(playerId)));

        AbstractQuestProgression<?> read = storage.loadProgression(quest.getId());
        assertNotNull(read);
        assertTrue(read.isAbandonedBy(playerId));
    }

    @Test
    void droppingAPlayerDropsTheLink() {
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        TestQuestProgression quest = quest("Shared", alice, bob);

        storage.saveProgressions(List.of(quest));

        quest.getPlayers().remove(bob);
        storage.saveProgressions(List.of(quest));

        assertEquals(Set.of(quest.getId()), ids(storage.loadPlayerProgressions(alice)));
        assertTrue(storage.loadPlayerProgressions(bob).isEmpty());
    }

    @Test
    void deletingAQuestTakesItsLinksWithIt() {
        UUID playerId = UUID.randomUUID();
        TestQuestProgression quest = quest("Gone", playerId);

        storage.saveProgressions(List.of(quest));
        storage.saveIndex("universe", Set.of(quest.getId()));

        storage.deleteProgression(quest);

        assertNull(storage.loadProgression(quest.getId()));
        assertTrue(storage.loadPlayerProgressions(playerId).isEmpty());
        assertTrue(storage.loadIndex("universe").isEmpty());
        assertTrue(storage.loadPlayer(playerId).getQuestIds().isEmpty());
    }

    @Test
    void anIndexIsReplacedWhole() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        storage.saveIndex("universe", Set.of(first, second));
        assertEquals(Set.of(first, second), storage.loadIndex("universe"));

        storage.saveIndex("universe", Set.of(second));
        assertEquals(Set.of(second), storage.loadIndex("universe"));

        storage.saveIndex("universe", Set.of());
        assertTrue(storage.loadIndex("universe").isEmpty());
    }

    @Test
    void indexesDoNotBleedIntoEachOther() {
        UUID universeQuest = UUID.randomUUID();
        UUID worldQuest = UUID.randomUUID();
        String worldKey = "world:" + UUID.randomUUID();

        storage.saveIndex("universe", Set.of(universeQuest));
        storage.saveIndex(worldKey, Set.of(worldQuest));

        assertEquals(Set.of(universeQuest), storage.loadIndex("universe"));
        assertEquals(Set.of(worldQuest), storage.loadIndex(worldKey));
    }

    @Test
    void writesAndReadsBackWhatAPlayerCarries() {
        UUID playerId = UUID.randomUUID();
        TestQuestProgression quest = quest("Owed", playerId);
        storage.saveProgressions(List.of(quest));

        PendingRewards owed = new PendingRewards(quest, new QuestReward[]{new TestQuestReward("berries")});

        storage.savePlayer(playerId, new PlayerQuestRecord(Set.of(), Set.of("StartHatchet", "PickBerries"), Set.of(owed), Map.of()));

        PlayerQuestRecord read = storage.loadPlayer(playerId);

        assertEquals(Set.of("StartHatchet", "PickBerries"), read.getStartedOnConnection());
        assertEquals(1, read.getPendingRewards().size());

        PendingRewards readOwed = read.getPendingRewards().iterator().next();
        assertEquals(quest.getId(), readOwed.getQuestId());
        assertEquals("berries", ((TestQuestReward) readOwed.getRewards()[0]).getLabel());

        // The link table answers for the quest ids, not the copy the record was written with
        assertEquals(Set.of(quest.getId()), read.getQuestIds());
    }

    @Test
    void addingADebtLeavesTheRestOfTheRecordAlone() {
        UUID playerId = UUID.randomUUID();
        TestQuestProgression quest = quest("Owed", playerId);
        storage.saveProgressions(List.of(quest));

        storage.savePlayer(playerId, new PlayerQuestRecord(Set.of(), Set.of("StartHatchet"), Set.of(), Map.of()));

        storage.addPendingRewards(playerId, new PendingRewards(quest, new QuestReward[]{new TestQuestReward("berries")}));

        PlayerQuestRecord read = storage.loadPlayer(playerId);
        assertEquals(Set.of("StartHatchet"), read.getStartedOnConnection());
        assertEquals(1, read.getPendingRewards().size());

        // The same completion, now owing less: it replaces rather than piling up
        storage.addPendingRewards(playerId, new PendingRewards(quest, new QuestReward[0]));

        read = storage.loadPlayer(playerId);
        assertEquals(1, read.getPendingRewards().size());
        assertEquals(0, read.getPendingRewards().iterator().next().getRewards().length);
        assertEquals(Set.of("StartHatchet"), read.getStartedOnConnection());
    }

    @Test
    void writesAndReadsBackHowEachAssetEnded() {
        UUID playerId = UUID.randomUUID();
        Instant startedAt = Instant.ofEpochMilli(1_000);
        Instant completedAt = Instant.ofEpochMilli(5_000);

        PlayerQuestRecord record = new PlayerQuestRecord();
        record.recordCompletion("DailyWood", QuestState.SUCCESSFUL, startedAt, completedAt);
        storage.savePlayer(playerId, record);

        QuestCompletions read = storage.loadPlayer(playerId).getCompletions().get("DailyWood");

        assertNotNull(read);
        assertEquals(1, read.count(QuestState.SUCCESSFUL));
        assertEquals(0, read.count(QuestState.FAILED));
        assertEquals(startedAt, read.getLastStartedAt());
        assertEquals(completedAt, read.getLastCompletedAt());
    }

    @Test
    void recordingACompletionLeavesTheRestOfTheRecordAlone() {
        UUID playerId = UUID.randomUUID();
        storage.savePlayer(playerId, new PlayerQuestRecord(Set.of(), Set.of("StartHatchet"), Set.of(), Map.of()));

        storage.recordCompletion(playerId, "DailyWood", QuestState.SUCCESSFUL, Instant.ofEpochMilli(1_000), Instant.ofEpochMilli(2_000));
        storage.recordCompletion(playerId, "DailyWood", QuestState.ABANDONED, Instant.ofEpochMilli(3_000), Instant.ofEpochMilli(4_000));

        PlayerQuestRecord read = storage.loadPlayer(playerId);
        QuestCompletions completions = read.getCompletions().get("DailyWood");

        assertEquals(Set.of("StartHatchet"), read.getStartedOnConnection());
        assertNotNull(completions);
        assertEquals(1, completions.count(QuestState.SUCCESSFUL));
        assertEquals(1, completions.count(QuestState.ABANDONED));
        assertEquals(2, completions.total());
        assertEquals(Instant.ofEpochMilli(4_000), completions.getLastCompletedAt());
    }

    @Test
    void recordingACompletionForAPlayerNobodyHasHeardOfStartsTheirRecord() {
        UUID playerId = UUID.randomUUID();

        storage.recordCompletion(playerId, "DailyWood", QuestState.FAILED, null, Instant.ofEpochMilli(2_000));

        PlayerQuestRecord read = storage.loadPlayer(playerId);
        assertFalse(read.isEmpty());
        assertEquals(1, read.getCompletions().get("DailyWood").count(QuestState.FAILED));
        assertNull(read.getCompletions().get("DailyWood").getLastStartedAt());
    }

    @Test
    void aPlayerNobodyHasHeardOfIsEmptyRatherThanMissing() {
        PlayerQuestRecord read = storage.loadPlayer(UUID.randomUUID());

        assertNotNull(read);
        assertTrue(read.isEmpty());
    }

    @Test
    void readsNothingForAnIdNothingAnswersTo() {
        assertNull(storage.loadProgression(UUID.randomUUID()));
        assertTrue(storage.loadProgressions(List.of(UUID.randomUUID(), UUID.randomUUID())).isEmpty());
        assertTrue(storage.loadProgressions(List.of()).isEmpty());
    }

    @Test
    void anIndexSurvivesAReconnection() {
        String url = "jdbc:h2:mem:openquests-restart;DB_CLOSE_DELAY=-1";
        UUID questId = UUID.randomUUID();

        QuestStorage first = open(url, null, null);
        first.saveIndex("universe", Set.of(questId));
        first.close();

        QuestStorage second = open(url, null, null);
        try {
            assertEquals(Set.of(questId), second.loadIndex("universe"));
        } finally {
            second.close();
        }
    }

    /**
     * SQLite spells its upsert the way PostgreSQL does, so this is the {@code ON CONFLICT}
     * statement under test — on a file, since several connections to one in-memory SQLite are
     * several databases.
     */
    @Test
    void worksOnADialectWithAnUpsert(@TempDir Path directory) {
        QuestStorage sqlite = open("jdbc:sqlite:" + directory.resolve("openquests.db"), null, null, 1);

        try {
            assertEquals(SqlDialect.SQLITE, SqlDialect.fromUrl("jdbc:sqlite:x"));
            assertTrue(SqlDialect.SQLITE.supportsUpsert());

            exerciseUpsertPath(sqlite);
        } finally {
            sqlite.close();
        }
    }

    /**
     * The same run against PostgreSQL, when {@code docker/postgres.yml} is up and its URL handed
     * in.
     */
    @Test
    @EnabledIfSystemProperty(named = PROPERTY_URL, matches = "jdbc:.+")
    void worksAgainstPostgreSql() {
        String url = System.getProperty(PROPERTY_URL);

        QuestStorage postgres = open(url,
            System.getProperty("openquests.jdbc.user"),
            System.getProperty("openquests.jdbc.password"),
            4);

        try {
            assertEquals(SqlDialect.POSTGRESQL, SqlDialect.fromUrl(url));

            exerciseUpsertPath(postgres);
        } finally {
            postgres.close();
        }
    }

    /**
     * Everything the H2 run covers one assertion at a time, in the order a session goes through
     * it, against a dialect that updates in place rather than deleting first.
     */
    private void exerciseUpsertPath(@Nonnull QuestStorage target) {
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();

        TestQuestProgression quest = quest("Chain", alice, bob);
        quest.setCounter(3);

        target.saveProgressions(List.of(quest));

        // Twice, so the upsert is the statement being tested rather than the insert
        quest.setCounter(9).setState(QuestState.SUCCESSFUL);
        target.saveProgressions(List.of(quest));

        AbstractQuestProgression<?> read = target.loadProgression(quest.getId());
        assertNotNull(read);
        assertEquals(9, ((TestQuestProgression) read).getCounter());
        assertEquals(QuestState.SUCCESSFUL, read.getState());
        assertEquals(Set.of(alice, bob), read.getPlayers());

        assertEquals(Set.of(quest.getId()), ids(target.loadPlayerProgressions(alice)));

        target.saveIndex("universe", Set.of(quest.getId()));
        assertEquals(Set.of(quest.getId()), target.loadIndex("universe"));

        target.savePlayer(alice, new PlayerQuestRecord(Set.of(), Set.of("Chain"), Set.of(), Map.of()));
        target.savePlayer(alice, new PlayerQuestRecord(Set.of(), Set.of("Chain", "Other"), Set.of(), Map.of()));
        assertEquals(Set.of("Chain", "Other"), target.loadPlayer(alice).getStartedOnConnection());

        quest.getPlayers().remove(bob);
        target.saveProgressions(List.of(quest));
        assertTrue(target.loadPlayerProgressions(bob).isEmpty());

        target.deleteProgression(quest);
        assertNull(target.loadProgression(quest.getId()));
        assertTrue(target.loadIndex("universe").isEmpty());
        assertFalse(target.loadPlayer(alice).getQuestIds().contains(quest.getId()));
    }

    @Nonnull
    private static QuestStorage open(@Nonnull String url, String user, String password) {
        return open(url, user, password, 4);
    }

    @Nonnull
    private static QuestStorage open(@Nonnull String url, String user, String password, int poolSize) {
        QuestStorage storage = new JdbcQuestStorage(new JdbcQuestStorage.JdbcSettings(
            url, emptyToNull(user), emptyToNull(password), null, null,
            "oqtest_", poolSize, 10, true, "test-server", SqlDialect.fromUrl(url)));

        storage.start();
        return storage;
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    @Nonnull
    private static TestQuestProgression quest(@Nonnull String assetId, @Nonnull UUID... playerIds) {
        TestQuestProgression quest = new TestQuestProgression();
        quest.setAssetId(assetId);
        quest.getPlayers().addAll(Set.of(playerIds));

        // The one field a record cannot invent later
        quest.onRegistered();

        return quest;
    }

    @Nonnull
    private static Set<UUID> ids(@Nonnull List<AbstractQuestProgression<?>> quests) {
        return quests.stream().map(AbstractQuestProgression::getId).collect(Collectors.toSet());
    }
}
