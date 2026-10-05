package com.martelstudios.openquests.core.persistence;

import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.AssignmentRecord;
import com.martelstudios.openquests.core.replication.Membership;
import com.martelstudios.openquests.core.replication.Replica;
import com.martelstudios.openquests.core.replication.StoredState;
import com.martelstudios.openquests.core.models.QuestCompletions;
import com.martelstudios.openquests.core.models.QuestScope;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.persistence.jdbc.JdbcQuestStorage;
import com.martelstudios.openquests.core.persistence.jdbc.SqlDialect;
import com.martelstudios.openquests.core.rewards.QuestReward;
import com.martelstudios.openquests.core.rewards.models.PendingRewards;
import com.martelstudios.openquests.core.scopes.world.WorldQuestScope;
import com.martelstudios.openquests.core.scopes.world.WorldsQuestScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.stream.Collectors;
import java.util.HashMap;
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
        QuestScope.CODEC.register(WorldQuestScope.TYPE, WorldQuestScope.class, WorldQuestScope.CODEC);
        QuestScope.CODEC.register(WorldsQuestScope.TYPE, WorldsQuestScope.class, WorldsQuestScope.CODEC);
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
    void aSharedHolderIsHandedAnOccasionOnce() {
        AssignmentRecord first = new AssignmentRecord(1, 1790476800000L, "period:2026-10-10T18:00:00Z");

        assertTrue(storage.claimAssignment("universe", "WeeklyHunt", "WeeklyHunt", null, first));
        assertFalse(storage.claimAssignment("universe", "WeeklyHunt", "WeeklyHunt", null, first));

        assertEquals(first, storage.loadAssignments("universe").get("WeeklyHunt", "WeeklyHunt"));
    }

    @Test
    void aRecordReadBeforeAnotherServerWroteIsRefused() {
        AssignmentRecord first = new AssignmentRecord(1, 1790476800000L, "period:2026-10-10T18:00:00Z");
        AssignmentRecord second = new AssignmentRecord(2, 1791081600000L, "period:2026-10-17T18:00:00Z");
        storage.claimAssignment("universe", "WeeklyHunt", "WeeklyHunt", null, first);

        assertTrue(storage.claimAssignment("universe", "WeeklyHunt", "WeeklyHunt", first, second));
        assertFalse(storage.claimAssignment("universe", "WeeklyHunt", "WeeklyHunt", first, second));

        assertEquals(second, storage.loadAssignments("universe").get("WeeklyHunt", "WeeklyHunt"));
    }

    @Test
    void aClosedWorldLetsGoOfItsRecords() {
        String lair = "world:" + UUID.randomUUID();
        storage.claimAssignment(lair, "GoblinLair", "GoblinLairRats", null, new AssignmentRecord(1, 1790476800000L, "once"));
        storage.claimAssignment("universe", "WeeklyHunt", "WeeklyHunt", null, new AssignmentRecord(1, 1790476800000L, "once"));

        storage.deleteAssignments(lair);

        assertTrue(storage.loadAssignments(lair).isEmpty());
        assertFalse(storage.loadAssignments("universe").isEmpty());
    }

    @Test
    void aSharedQuestKeepsWhoSharesIt() {
        UUID lair = UUID.randomUUID();
        UUID arena = UUID.randomUUID();
        TestQuestProgression quest = quest("ClearTheRats", UUID.randomUUID());
        WorldQuestScope scope = new WorldQuestScope(List.of(lair));
        scope.addWorld(arena);
        quest.setScope(scope);

        storage.saveProgressions(List.of(quest));

        AbstractQuestProgression<?> read = storage.loadProgression(quest.getId());

        assertNotNull(read);
        assertTrue(read.getScope() instanceof WorldQuestScope);
        assertEquals(Set.of(lair, arena), ((WorldQuestScope) read.getScope()).getWorlds());
    }

    @Test
    void aQuestAGroupSharesKeepsItsGroupAndTheWorldsItReached() {
        UUID arena = UUID.randomUUID();
        TestQuestProgression quest = quest("ArenaGoal", UUID.randomUUID());
        WorldsQuestScope scope = new WorldsQuestScope("ArenaTogether");
        scope.addWorld(arena);
        quest.setScope(scope);

        storage.saveProgressions(List.of(quest));

        AbstractQuestProgression<?> read = storage.loadProgression(quest.getId());

        assertNotNull(read);
        assertTrue(read.getScope() instanceof WorldsQuestScope);
        assertEquals("ArenaTogether", ((WorldsQuestScope) read.getScope()).getGroup());
        assertEquals(Set.of(arena), ((WorldsQuestScope) read.getScope()).getWorlds());
    }

    @Test
    void aQuestNobodySharesHasNoScope() {
        TestQuestProgression quest = quest("CollectStick", UUID.randomUUID());

        storage.saveProgressions(List.of(quest));

        AbstractQuestProgression<?> read = storage.loadProgression(quest.getId());

        assertNotNull(read);
        assertNull(read.getScope());
    }

    @Test
    void savingTheSameQuestTwiceUpdatesIt() {
        TestQuestProgression quest = quest("CollectStick", UUID.randomUUID());

        storage.saveProgressions(List.of(quest));

        quest.setCounter(42).restoreStoredState(new StoredState(QuestState.SUCCESSFUL, Instant.now(), 1));
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
        quest.move(playerId, Membership.Status.ABANDONED);

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

        quest.move(bob, Membership.Status.LEFT);
        storage.saveProgressions(List.of(quest));

        assertEquals(Set.of(quest.getId()), ids(storage.loadPlayerProgressions(alice)));
        assertTrue(storage.loadPlayerProgressions(bob).isEmpty());
    }

    @Test
    void deletingAQuestTakesItsLinksWithIt() {
        UUID playerId = UUID.randomUUID();
        TestQuestProgression quest = quest("Gone", playerId);

        storage.saveProgressions(List.of(quest));
        storage.addToIndex("universe", Set.of(quest.getId()));

        storage.deleteProgression(quest);

        assertNull(storage.loadProgression(quest.getId()));
        assertTrue(storage.loadPlayerProgressions(playerId).isEmpty());
        assertTrue(storage.loadIndex("universe").isEmpty());
        assertTrue(storage.loadPlayer(playerId).getQuestIds().isEmpty());
    }

    @Test
    void anIndexIsWrittenMemberByMember() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        storage.addToIndex("universe", Set.of(first, second));
        storage.addToIndex("universe", Set.of(first));
        assertEquals(Set.of(first, second), storage.loadIndex("universe"));

        storage.removeFromIndex("universe", Set.of(first));
        assertEquals(Set.of(second), storage.loadIndex("universe"));

        storage.removeFromIndex("universe", Set.of(first, second));
        assertTrue(storage.loadIndex("universe").isEmpty());
    }

    @Test
    void indexesDoNotBleedIntoEachOther() {
        UUID universeQuest = UUID.randomUUID();
        UUID worldQuest = UUID.randomUUID();
        String worldKey = "world:" + UUID.randomUUID();

        storage.addToIndex("universe", Set.of(universeQuest));
        storage.addToIndex(worldKey, Set.of(worldQuest));

        assertEquals(Set.of(universeQuest), storage.loadIndex("universe"));
        assertEquals(Set.of(worldQuest), storage.loadIndex(worldKey));
    }

    @Test
    void writesAndReadsBackWhatAPlayerCarries() {
        UUID playerId = UUID.randomUUID();
        TestQuestProgression quest = quest("Owed", playerId);
        storage.saveProgressions(List.of(quest));

        PendingRewards owed = new PendingRewards(quest, new QuestReward[]{new TestQuestReward("berries")});

        storage.savePlayer(playerId, new PlayerQuestRecord(Set.of(), handed("OnConnection", "StartHatchet", "PickBerries"), Set.of(owed), Map.of()), List.of());

        PlayerQuestRecord read = storage.loadPlayer(playerId);

        assertEquals(handed("OnConnection", "StartHatchet", "PickBerries"), read.getAssignments());
        assertEquals(1, read.getPendingRewards().size());

        PendingRewards readOwed = read.getPendingRewards().iterator().next();
        assertEquals(quest.getId(), readOwed.getQuestId());
        assertEquals("berries", ((TestQuestReward) readOwed.getRewards()[0]).getLabel());

        // The link table answers for the quest ids, not the copy the record was written with
        assertEquals(Set.of(quest.getId()), read.getQuestIds());
    }

    @Test
    void aDebtLeftForAPlayerWaitsUntilTheirRecordTakesItIn() {
        UUID playerId = UUID.randomUUID();
        TestQuestProgression quest = quest("Owed", playerId);
        storage.saveProgressions(List.of(quest));
        storage.savePlayer(playerId, new PlayerQuestRecord(Set.of(), handed("OnConnection", "StartHatchet"), Set.of(), Map.of()), List.of());

        storage.postMessage(PlayerMessage.owed(playerId, new PendingRewards(quest, new QuestReward[]{new TestQuestReward("berries")})));
        // The same completion, now owing less: taken in, it replaces rather than piling up
        storage.postMessage(PlayerMessage.owed(playerId, new PendingRewards(quest, new QuestReward[0])));

        PlayerQuestRecord read = takeInMessages(storage, playerId);

        assertEquals(handed("OnConnection", "StartHatchet"), read.getAssignments());
        assertEquals(1, read.getPendingRewards().size());
        assertEquals(0, read.getPendingRewards().iterator().next().getRewards().length);
        assertTrue(storage.loadMessages(List.of(playerId)).isEmpty());
    }

    @Test
    void writesAndReadsBackHowEachAssetEnded() {
        UUID playerId = UUID.randomUUID();
        Instant startedAt = Instant.ofEpochMilli(1_000);
        Instant completedAt = Instant.ofEpochMilli(5_000);

        PlayerQuestRecord record = new PlayerQuestRecord();
        record.recordCompletion("DailyWood", QuestState.SUCCESSFUL, startedAt, completedAt);
        storage.savePlayer(playerId, record, List.of());

        QuestCompletions read = storage.loadPlayer(playerId).getCompletions().get("DailyWood");

        assertNotNull(read);
        assertEquals(1, read.count(QuestState.SUCCESSFUL));
        assertEquals(0, read.count(QuestState.FAILED));
        assertEquals(startedAt, read.getLastStartedAt());
        assertEquals(completedAt, read.getLastCompletedAt());
    }

    @Test
    void anEndingLeftForAPlayerCountsLikeOneSeenThere() {
        UUID playerId = UUID.randomUUID();
        storage.savePlayer(playerId, new PlayerQuestRecord(Set.of(), handed("OnConnection", "StartHatchet"), Set.of(), Map.of()), List.of());

        storage.postMessage(PlayerMessage.ended(playerId, "DailyWood", QuestState.SUCCESSFUL, Instant.ofEpochMilli(1_000), Instant.ofEpochMilli(2_000)));
        storage.postMessage(PlayerMessage.ended(playerId, "DailyWood", QuestState.ABANDONED, Instant.ofEpochMilli(3_000), Instant.ofEpochMilli(4_000)));

        PlayerQuestRecord read = takeInMessages(storage, playerId);
        QuestCompletions completions = read.getCompletions().get("DailyWood");

        assertEquals(handed("OnConnection", "StartHatchet"), read.getAssignments());
        assertNotNull(completions);
        assertEquals(1, completions.count(QuestState.SUCCESSFUL));
        assertEquals(1, completions.count(QuestState.ABANDONED));
        assertEquals(2, completions.total());
        assertEquals(Instant.ofEpochMilli(4_000), completions.getLastCompletedAt());
    }

    @Test
    void anEndingLeftForAPlayerNobodyHasHeardOfStartsTheirRecord() {
        UUID playerId = UUID.randomUUID();

        storage.postMessage(PlayerMessage.ended(playerId, "DailyWood", QuestState.FAILED, null, Instant.ofEpochMilli(2_000)));

        PlayerQuestRecord read = takeInMessages(storage, playerId);
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
        first.addToIndex("universe", Set.of(questId));
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
     * statement under test, run on a file since several connections to one in-memory SQLite are
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
        quest.setCounter(9).restoreStoredState(new StoredState(QuestState.SUCCESSFUL, Instant.now(), 1));
        target.saveProgressions(List.of(quest));

        AbstractQuestProgression<?> read = target.loadProgression(quest.getId());
        assertNotNull(read);
        assertEquals(9, ((TestQuestProgression) read).getCounter());
        assertEquals(QuestState.SUCCESSFUL, read.getState());
        assertEquals(Set.of(alice, bob), read.getPlayers());

        assertEquals(Set.of(quest.getId()), ids(target.loadPlayerProgressions(alice)));

        target.addToIndex("universe", Set.of(quest.getId()));
        assertEquals(Set.of(quest.getId()), target.loadIndex("universe"));

        target.savePlayer(alice, new PlayerQuestRecord(Set.of(), handed("OnConnection", "Chain"), Set.of(), Map.of()), List.of());
        target.savePlayer(alice, new PlayerQuestRecord(Set.of(), handed("OnConnection", "Chain", "Other"), Set.of(), Map.of()), List.of());
        assertEquals(handed("OnConnection", "Chain", "Other"), target.loadPlayer(alice).getAssignments());

        quest.move(bob, Membership.Status.LEFT);
        target.saveProgressions(List.of(quest));
        assertTrue(target.loadPlayerProgressions(bob).isEmpty());

        target.deleteProgression(quest);
        assertNull(target.loadProgression(quest.getId()));
        assertTrue(target.loadIndex("universe").isEmpty());
        assertFalse(target.loadPlayer(alice).getQuestIds().contains(quest.getId()));
    }

    @Test
    void twoServersProgressingOneQuestKeepEachOthersShare() {
        String url = "jdbc:h2:mem:openquests-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        QuestStorage onA = open(url, "a");
        QuestStorage onB = open(url, "b");

        try {
            UUID alice = UUID.randomUUID();
            UUID bob = UUID.randomUUID();
            TestQuestProgression quest = new TestQuestProgression();
            quest.setAssetId("CommunityHunt");
            quest.onRegistered();

            Replica.setLocalId("a");
            onA.saveProgressions(List.of(quest));
            TestQuestProgression copyOnB = (TestQuestProgression) onB.loadProgression(quest.getId());

            quest.move(alice, Membership.Status.JOINED).addShared(5);
            onA.saveProgressions(List.of(quest));

            Replica.setLocalId("b");
            copyOnB.move(bob, Membership.Status.JOINED).addShared(3);
            onB.saveProgressions(List.of(copyOnB));

            TestQuestProgression read = (TestQuestProgression) onA.loadProgression(quest.getId());

            assertEquals(8, read.getShared());
            assertEquals(Set.of(alice, bob), read.getPlayers());
        } finally {
            Replica.setLocalId(Replica.DEFAULT_ID);
            onA.close();
            onB.close();
        }
    }

    @Test
    void onlyOneServerMakesAChangeOfState() {
        String url = "jdbc:h2:mem:openquests-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        QuestStorage onA = open(url, "a");
        QuestStorage onB = open(url, "b");

        try {
            TestQuestProgression quest = quest("Race");
            onA.saveProgressions(List.of(quest));
            TestQuestProgression copyOnB = (TestQuestProgression) onB.loadProgression(quest.getId());

            assertNull(change(onA, quest, QuestState.SUCCESSFUL));
            assertEquals(QuestState.SUCCESSFUL, change(onB, copyOnB, QuestState.FAILED).state());
            assertEquals(QuestState.SUCCESSFUL, copyOnB.getState());

            // Whatever its own replica says, a copy read back stands where the claims left it
            copyOnB.setState(QuestState.FAILED);
            onB.saveProgressions(List.of(copyOnB));
            assertEquals(QuestState.SUCCESSFUL, onB.loadProgression(quest.getId()).getState());
        } finally {
            onA.close();
            onB.close();
        }
    }

    @Test
    void aQuestDoneAwayWithOnceEndedStaysEndedForTheOthers() {
        String url = "jdbc:h2:mem:openquests-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        QuestStorage onA = open(url, "a");
        QuestStorage onB = open(url, "b");

        try {
            TestQuestProgression quest = quest("Unheld");
            onA.saveProgressions(List.of(quest));
            TestQuestProgression copyOnB = (TestQuestProgression) onB.loadProgression(quest.getId());

            assertNull(change(onA, quest, QuestState.SUCCESSFUL));
            onA.deleteProgression(quest);
            assertNull(onA.loadProgression(quest.getId()));

            // B, still running it, hears of the end, and its own end, even written first, loses
            assertEquals(QuestState.SUCCESSFUL, onB.pollReplicas(Map.of(quest.getId(), Map.of())).states().get(quest.getId()).state());
            onB.saveProgressions(List.of(copyOnB));
            assertEquals(QuestState.SUCCESSFUL, change(onB, copyOnB, QuestState.FAILED).state());
        } finally {
            onA.close();
            onB.close();
        }
    }

    @Test
    void aQuestKeptRunningReopensAndEndsAgainAndACopyBehindFollows() {
        String url = "jdbc:h2:mem:openquests-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        QuestStorage onA = open(url, "a");
        QuestStorage onB = open(url, "b");

        try {
            TestQuestProgression quest = quest("Holding");
            onA.saveProgressions(List.of(quest));
            TestQuestProgression copyOnB = (TestQuestProgression) onB.loadProgression(quest.getId());

            assertNull(change(onA, quest, QuestState.SUCCESSFUL));
            assertNull(change(onA, quest, QuestState.IN_PROGRESS));
            assertNull(change(onA, quest, QuestState.FAILED));

            // Three changes behind, B is told where the quest stands rather than making its own
            StoredState stored = change(onB, copyOnB, QuestState.SUCCESSFUL);
            assertEquals(QuestState.FAILED, stored.state());
            assertEquals(3, stored.epoch());
            assertEquals(QuestState.FAILED, onB.loadProgression(quest.getId()).getState());
        } finally {
            onA.close();
            onB.close();
        }
    }

    @Test
    void aChangeOnlyThisServerMakesIsNeverUndoneByAReload() {
        TestQuestProgression quest = quest("NeverFailed");

        // A quest this server alone moves claims nothing: where it stands goes out with its replica
        quest.restoreStoredState(new StoredState(QuestState.SUCCESSFUL, Instant.now(), 1));
        storage.saveProgressions(List.of(quest));
        quest.restoreStoredState(new StoredState(QuestState.FAILED, Instant.now(), 2));
        storage.saveProgressions(List.of(quest));
        assertEquals(QuestState.FAILED, storage.loadProgression(quest.getId()).getState());

        // An older copy written late never takes it back
        AbstractQuestProgression<?> late = storage.loadProgression(quest.getId());
        late.restoreStoredState(new StoredState(QuestState.SUCCESSFUL, Instant.now(), 1));
        storage.saveProgressions(List.of(late));
        assertEquals(QuestState.FAILED, storage.loadProgression(quest.getId()).getState());

        // Back to running, it reads back running, with no end date left standing
        quest.restoreStoredState(new StoredState(QuestState.IN_PROGRESS, null, 3));
        storage.saveProgressions(List.of(quest));
        AbstractQuestProgression<?> reopened = storage.loadProgression(quest.getId());
        assertEquals(QuestState.IN_PROGRESS, reopened.getState());
        assertNull(reopened.getCompletedAt());
        assertEquals(3, reopened.getStateEpoch());
    }

    @Test
    void anOutcomeNobodyNeedsAnyMoreIsDroppedAtStart() {
        String url = "jdbc:h2:mem:openquests-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        QuestStorage onA = open(url, "a");

        TestQuestProgression longAgo = quest("LongAgo");
        TestQuestProgression lately = quest("Lately");
        TestQuestProgression held = quest("Held", UUID.randomUUID());
        onA.saveProgressions(List.of(longAgo, lately, held));

        Instant twoDaysAgo = Instant.now().minus(Duration.ofDays(2));
        longAgo.restoreStoredState(new StoredState(QuestState.SUCCESSFUL, twoDaysAgo, 1));
        lately.restoreStoredState(new StoredState(QuestState.SUCCESSFUL, Instant.now(), 1));
        held.restoreStoredState(new StoredState(QuestState.SUCCESSFUL, twoDaysAgo, 1));
        onA.saveProgressions(List.of(held));
        onA.deleteProgression(longAgo);
        onA.deleteProgression(lately);
        onA.close();

        QuestStorage onB = open(url, "b");
        try {
            Map<UUID, Map<String, Long>> asked = Map.of(longAgo.getId(), Map.of(), lately.getId(), Map.of(), held.getId(), Map.of());
            assertEquals(Set.of(lately.getId(), held.getId()), onB.pollReplicas(asked).states().keySet());
            assertNotNull(onB.loadProgression(held.getId()));
        } finally {
            onB.close();
        }
    }

    @Test
    void aPollBringsWhatAnotherServerWroteAndOnlyThat() {
        String url = "jdbc:h2:mem:openquests-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        QuestStorage onA = open(url, "a");
        QuestStorage onB = open(url, "b");

        try {
            TestQuestProgression quest = quest("Polled");
            onA.saveProgressions(List.of(quest));

            ReplicaPoll first = onB.pollReplicas(Map.of(quest.getId(), Map.of()));
            assertEquals(1, first.changed().size());
            assertEquals("a", first.changed().getFirst().serverId());

            long seen = first.changed().getFirst().revision();
            assertTrue(onB.pollReplicas(Map.of(quest.getId(), Map.of("a", seen))).changed().isEmpty());

            onA.saveProgressions(List.of(quest));
            ReplicaPoll second = onB.pollReplicas(Map.of(quest.getId(), Map.of("a", seen)));
            assertEquals(1, second.changed().size());
            assertTrue(second.changed().getFirst().revision() > seen);

            change(onA, quest, QuestState.SUCCESSFUL);
            assertEquals(new StoredState(QuestState.SUCCESSFUL, quest.getCompletedAt(), 1), onB.pollReplicas(Map.of(quest.getId(), Map.of("a", Long.MAX_VALUE))).states().get(quest.getId()));

            // Nothing of a server's own replicas comes back to it
            assertTrue(onA.pollReplicas(Map.of(quest.getId(), Map.of())).changed().isEmpty());
        } finally {
            onA.close();
            onB.close();
        }
    }

    @Test
    void twoServersAddingToOneIndexKeepBoth() {
        String url = "jdbc:h2:mem:openquests-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        QuestStorage onA = open(url, "a");
        QuestStorage onB = open(url, "b");

        try {
            UUID fromA = UUID.randomUUID();
            UUID fromB = UUID.randomUUID();

            onA.addToIndex("universe", Set.of(fromA));
            onB.addToIndex("universe", Set.of(fromB));
            assertEquals(Set.of(fromA, fromB), onA.loadIndex("universe"));

            onA.removeFromIndex("universe", Set.of(fromA));
            assertEquals(Map.of("universe", Set.of(fromB), "worlds:Arena", Set.of()), onB.loadIndexes(List.of("universe", "worlds:Arena")));
        } finally {
            onA.close();
            onB.close();
        }
    }

    @Test
    void tablesAnOlderVersionWroteAreRefused() {
        String url = "jdbc:h2:mem:openquests-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        QuestStorage first = open(url, "a");
        first.close();

        try (java.sql.Connection connection = java.sql.DriverManager.getConnection(url);
             java.sql.Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE oqtest_schema_version");
        } catch (java.sql.SQLException e) {
            throw new AssertionError(e);
        }

        org.junit.jupiter.api.Assertions.assertThrows(QuestStorageException.class, () -> open(url, "a"));
    }

    /**
     * What a server does as a player connects: reads their record, takes in what was left for them,
     * and writes the record back, which lets go of the messages.
     */
    @Nonnull
    private static PlayerQuestRecord takeInMessages(@Nonnull QuestStorage storage, @Nonnull UUID playerId) {
        PlayerQuestRecord record = storage.loadPlayer(playerId);
        List<UUID> delivered = new java.util.ArrayList<>();

        for (PlayerMessage message : storage.loadMessages(List.of(playerId))) {
            message.applyTo(record);
            delivered.add(message.getId());
        }

        storage.savePlayer(playerId, record, delivered);
        return storage.loadPlayer(playerId);
    }

    /**
     * Moves a copy the way a quest changing state does: claimed from the epoch it stood at, then
     * put where the storage says it stands, its own change or the one another server made first.
     *
     * @return {@code null} if the change was this copy's to make.
     */
    @Nullable
    private static StoredState change(@Nonnull QuestStorage on, @Nonnull TestQuestProgression quest, @Nonnull QuestState state) {
        long from = quest.getStateEpoch();
        Instant at = state == QuestState.IN_PROGRESS ? null : Instant.ofEpochMilli(System.currentTimeMillis());
        quest.restoreStoredState(new StoredState(state, at, from));

        StoredState stored = on.claimState(quest);
        quest.restoreStoredState(stored != null ? stored : new StoredState(state, at, from + 1));
        return stored;
    }

    @Nonnull
    private static QuestStorage open(@Nonnull String url, @Nonnull String serverId) {
        QuestStorage storage = new JdbcQuestStorage(new JdbcQuestStorage.JdbcSettings(
            url, null, null, null, null, "oqtest_", 4, 10, true, serverId, SqlDialect.fromUrl(url)));

        storage.start();
        return storage;
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
    private static Map<String, Map<String, AssignmentRecord>> handed(@Nonnull String assignmentId, @Nonnull String... questAssetIds) {
        Map<String, AssignmentRecord> byQuest = new HashMap<>();
        for (String questAssetId : questAssetIds) {
            byQuest.put(questAssetId, new AssignmentRecord(1, 1790476800000L, "once"));
        }
        return Map.of(assignmentId, byQuest);
    }

    private static TestQuestProgression quest(@Nonnull String assetId, @Nonnull UUID... playerIds) {
        TestQuestProgression quest = new TestQuestProgression();
        quest.setAssetId(assetId);
        for (UUID playerId : playerIds) quest.move(playerId, Membership.Status.JOINED);

        // The one field a record cannot invent later
        quest.onRegistered();

        return quest;
    }

    @Nonnull
    private static Set<UUID> ids(@Nonnull List<AbstractQuestProgression<?>> quests) {
        return quests.stream().map(AbstractQuestProgression::getId).collect(Collectors.toSet());
    }
}
