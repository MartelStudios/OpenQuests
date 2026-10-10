package com.martelstudios.openquests.core.persistence;

import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.AssignmentRecord;
import com.martelstudios.openquests.core.models.QuestOperation;
import com.martelstudios.openquests.core.models.QuestCompletions;
import com.martelstudios.openquests.core.models.QuestScope;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.persistence.jdbc.JdbcQuestStorage;
import com.martelstudios.openquests.core.persistence.jdbc.SqlDialect;
import com.martelstudios.openquests.core.rewards.QuestReward;
import com.martelstudios.openquests.core.rewards.models.PendingRewards;
import com.martelstudios.openquests.core.scopes.world.WorldQuestScope;
import com.martelstudios.openquests.core.scopes.world.WorldsQuestScope;
import com.martelstudios.openquests.core.sync.QuestSync;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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
        quest.setCounter(7).end(QuestState.IN_PROGRESS).addTag("OQ_TEST", "a", "b");

        storage.writeProgressions(List.of(quest));

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

        storage.writeProgressions(List.of(quest));

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

        storage.writeProgressions(List.of(quest));

        AbstractQuestProgression<?> read = storage.loadProgression(quest.getId());

        assertNotNull(read);
        assertTrue(read.getScope() instanceof WorldsQuestScope);
        assertEquals("ArenaTogether", ((WorldsQuestScope) read.getScope()).getGroup());
        assertEquals(Set.of(arena), ((WorldsQuestScope) read.getScope()).getWorlds());
    }

    @Test
    void aQuestNobodySharesHasNoScope() {
        TestQuestProgression quest = quest("CollectStick", UUID.randomUUID());

        storage.writeProgressions(List.of(quest));

        AbstractQuestProgression<?> read = storage.loadProgression(quest.getId());

        assertNotNull(read);
        assertNull(read.getScope());
    }

    @Test
    void savingTheSameQuestTwiceUpdatesIt() {
        TestQuestProgression quest = quest("CollectStick", UUID.randomUUID());

        storage.writeProgressions(List.of(quest));

        quest.setCounter(42).end(QuestState.SUCCESSFUL);
        storage.writeProgressions(List.of(quest));

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

        storage.writeProgressions(List.of(hers, his, shared));

        assertEquals(Set.of(hers.getId(), shared.getId()), ids(storage.loadPlayerProgressions(alice)));
        assertEquals(Set.of(his.getId(), shared.getId()), ids(storage.loadPlayerProgressions(bob)));
    }

    @Test
    void aPlayerWhoGaveUpStillHoldsTheQuest() {
        UUID playerId = UUID.randomUUID();
        TestQuestProgression quest = quest("Abandoned", playerId);
        quest.giveUp(playerId);

        storage.writeProgressions(List.of(quest));

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

        storage.writeProgressions(List.of(quest));

        quest.leave(bob);
        storage.writeProgressions(List.of(quest));

        assertEquals(Set.of(quest.getId()), ids(storage.loadPlayerProgressions(alice)));
        assertTrue(storage.loadPlayerProgressions(bob).isEmpty());
    }

    @Test
    void deletingAQuestTakesItsLinksWithIt() {
        UUID playerId = UUID.randomUUID();
        TestQuestProgression quest = quest("Gone", playerId);

        storage.writeProgressions(List.of(quest));
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
        storage.writeProgressions(List.of(quest));

        PendingRewards owed = new PendingRewards(quest, new QuestReward[]{new TestQuestReward("berries")});

        storage.savePlayer(playerId, new PlayerQuestRecord(Set.of(), handed("OnConnection", "StartHatchet", "PickBerries"), Set.of(owed), Map.of(), Map.of()), List.of(), true);

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
        storage.writeProgressions(List.of(quest));
        storage.savePlayer(playerId, new PlayerQuestRecord(Set.of(), handed("OnConnection", "StartHatchet"), Set.of(), Map.of(), Map.of()), List.of(), true);

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
    void eachHolderOfAQuestKeepsTheirOwnTracking() {
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        TestQuestProgression quest = quest("Shared", alice, bob);
        storage.writeProgressions(List.of(quest));

        storage.savePlayer(alice, new PlayerQuestRecord(Set.of(), Map.of(), Set.of(), Map.of(), Map.of(quest.getId(), true)), List.of(), true);
        storage.savePlayer(bob, new PlayerQuestRecord(Set.of(), Map.of(), Set.of(), Map.of(), Map.of(quest.getId(), false)), List.of(), true);

        assertEquals(Map.of(quest.getId(), true), storage.loadPlayer(alice).getTracking());
        assertEquals(Map.of(quest.getId(), false), storage.loadPlayer(bob).getTracking());
    }

    @Test
    void writesAndReadsBackHowEachAssetEnded() {
        UUID playerId = UUID.randomUUID();
        Instant startedAt = Instant.ofEpochMilli(1_000);
        Instant completedAt = Instant.ofEpochMilli(5_000);

        PlayerQuestRecord record = new PlayerQuestRecord();
        record.recordCompletion("DailyWood", QuestState.SUCCESSFUL, startedAt, completedAt);
        storage.savePlayer(playerId, record, List.of(), true);

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
        storage.savePlayer(playerId, new PlayerQuestRecord(Set.of(), handed("OnConnection", "StartHatchet"), Set.of(), Map.of(), Map.of()), List.of(), true);

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

        target.writeProgressions(List.of(quest));

        // Twice, so the upsert is the statement being tested rather than the insert
        quest.setCounter(9).end(QuestState.SUCCESSFUL);
        target.writeProgressions(List.of(quest));

        AbstractQuestProgression<?> read = target.loadProgression(quest.getId());
        assertNotNull(read);
        assertEquals(9, ((TestQuestProgression) read).getCounter());
        assertEquals(QuestState.SUCCESSFUL, read.getState());
        assertEquals(Set.of(alice, bob), read.getPlayers());

        assertEquals(Set.of(quest.getId()), ids(target.loadPlayerProgressions(alice)));

        target.addToIndex("universe", Set.of(quest.getId()));
        assertEquals(Set.of(quest.getId()), target.loadIndex("universe"));

        target.savePlayer(alice, new PlayerQuestRecord(Set.of(), handed("OnConnection", "Chain"), Set.of(), Map.of(), Map.of()), List.of(), true);
        target.savePlayer(alice, new PlayerQuestRecord(Set.of(), handed("OnConnection", "Chain", "Other"), Set.of(), Map.of(), Map.of()), List.of(), true);
        assertEquals(handed("OnConnection", "Chain", "Other"), target.loadPlayer(alice).getAssignments());

        quest.leave(bob);
        target.writeProgressions(List.of(quest));
        assertTrue(target.loadPlayerProgressions(bob).isEmpty());

        // Ended, it stays readable a while for servers that have not heard, but nobody holds it
        target.deleteProgression(quest);
        assertEquals(QuestState.SUCCESSFUL, target.loadProgression(quest.getId()).getState());
        assertTrue(target.loadIndex("universe").isEmpty());
        assertFalse(target.loadPlayer(alice).getQuestIds().contains(quest.getId()));
    }

    @Test
    void twoServersChangingOneQuestKeepBothChanges() {
        String url = "jdbc:h2:mem:openquests-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        QuestStorage onA = open(url);
        QuestStorage onB = open(url);
        QuestSync.setPolicy(SHARED_ONCE_STORED);

        try {
            UUID alice = UUID.randomUUID();
            UUID bob = UUID.randomUUID();
            TestQuestProgression quest = quest("CommunityHunt");
            onA.writeProgressions(List.of(quest));
            TestQuestProgression copyOnB = (TestQuestProgression) onB.loadProgression(quest.getId());

            quest.join(alice).addToCounter(5);
            copyOnB.join(bob).addToCounter(3);

            write(onA, quest);
            write(onB, copyOnB);

            TestQuestProgression read = (TestQuestProgression) onA.loadProgression(quest.getId());
            assertEquals(8, read.getCounter());
            assertEquals(Set.of(alice, bob), read.getPlayers());

            // B wrote last, from a copy that had not heard of Alice: her link stands all the same
            assertEquals(Set.of(quest.getId()), ids(onB.loadPlayerProgressions(alice)));
            assertEquals(Set.of(quest.getId()), ids(onA.loadPlayerProgressions(bob)));

            // The copy that wrote last stands on what it wrote
            assertEquals(8, copyOnB.getCounter());
            assertEquals(3, copyOnB.getStoredVersion());
        } finally {
            QuestSync.setPolicy(ALONE);
            onA.close();
            onB.close();
        }
    }

    @Test
    void aWriteOvertakenMidwayIsMadeAgainOnWhatOvertookIt() {
        String url = "jdbc:h2:mem:openquests-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        QuestStorage onA = open(url);
        QuestStorage onB = open(url);

        try {
            TestQuestProgression quest = quest("Race");
            onA.writeProgressions(List.of(quest));
            AtomicInteger attempts = new AtomicInteger();

            AbstractQuestProgression<?> written = onB.commitProgression(quest.getId(), stored -> {
                // A writes between B's read and B's write, once
                if (attempts.getAndIncrement() == 0) onA.commitProgression(quest.getId(), theirs -> addTo(theirs, 5));
                return addTo(stored, 3);
            });

            assertEquals(2, attempts.get());
            assertNotNull(written);
            assertEquals(8, ((TestQuestProgression) written).getCounter());
            assertEquals(8, ((TestQuestProgression) onA.loadProgression(quest.getId())).getCounter());
        } finally {
            onA.close();
            onB.close();
        }
    }

    @Test
    void anEndBothServersReachIsMadeByTheFirstToWrite() {
        String url = "jdbc:h2:mem:openquests-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        QuestStorage onA = open(url);
        QuestStorage onB = open(url);
        QuestSync.setPolicy(SHARED_ONCE_STORED);

        try {
            TestQuestProgression quest = quest("Race");
            onA.writeProgressions(List.of(quest));
            TestQuestProgression copyOnB = (TestQuestProgression) onB.loadProgression(quest.getId());

            quest.addAndEndAt(10, 10);
            copyOnB.addAndEndAt(10, 10);
            assertEquals(QuestState.SUCCESSFUL, copyOnB.getState());

            write(onA, quest);
            List<QuestState> foundByB = new ArrayList<>();
            List<QuestState> leftByB = new ArrayList<>();
            List<? extends QuestOperation<?>> changes = copyOnB.getPendingOperations();
            AbstractQuestProgression<?> written = onB.commitProgression(quest.getId(), stored -> {
                foundByB.add(stored.getState());
                boolean changed = replay(stored, changes);
                leftByB.add(stored.getState());
                return changed;
            });

            // B finds the quest ended already: the end is A's, and B makes no change of state
            assertEquals(List.of(QuestState.SUCCESSFUL), foundByB);
            assertEquals(List.of(QuestState.SUCCESSFUL), leftByB);
            assertNotNull(written);
        } finally {
            QuestSync.setPolicy(ALONE);
            onA.close();
            onB.close();
        }
    }

    @Test
    void aChangeAnotherServerMadeAlreadyWritesNothing() {
        String url = "jdbc:h2:mem:openquests-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        QuestStorage onA = open(url);
        QuestStorage onB = open(url);
        QuestSync.setPolicy(SHARED_ONCE_STORED);

        try {
            UUID alice = UUID.randomUUID();
            TestQuestProgression quest = quest("Joined");
            onA.writeProgressions(List.of(quest));
            TestQuestProgression copyOnB = (TestQuestProgression) onB.loadProgression(quest.getId());

            quest.join(alice);
            copyOnB.join(alice);
            write(onA, quest);
            write(onB, copyOnB);

            // B stands on what A wrote, and the others have nothing new to read
            assertEquals(Map.of(quest.getId(), 2L), onA.loadVersions(List.of(quest.getId())));
            assertEquals(2, copyOnB.getStoredVersion());
            assertFalse(copyOnB.hasChanges());
        } finally {
            QuestSync.setPolicy(ALONE);
            onA.close();
            onB.close();
        }
    }

    @Test
    void aCopyTakingOnAStoredOneKeepsWhatItHasStillToWrite() {
        String url = "jdbc:h2:mem:openquests-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        QuestStorage onA = open(url);
        QuestStorage onB = open(url);
        QuestSync.setPolicy(SHARED_ONCE_STORED);

        try {
            TestQuestProgression quest = quest("Holding");
            onA.writeProgressions(List.of(quest));
            TestQuestProgression copyOnB = (TestQuestProgression) onB.loadProgression(quest.getId());

            quest.addToCounter(5);
            write(onA, quest);

            copyOnB.addToCounter(3);
            copyOnB.restack(onB.loadProgression(quest.getId()), 0);
            assertEquals(8, copyOnB.getCounter());
            assertEquals(2, copyOnB.getStoredVersion());
            assertTrue(copyOnB.hasChanges());

            write(onB, copyOnB);
            assertEquals(8, ((TestQuestProgression) onA.loadProgression(quest.getId())).getCounter());
            assertFalse(copyOnB.hasChanges());
        } finally {
            QuestSync.setPolicy(ALONE);
            onA.close();
            onB.close();
        }
    }

    @Test
    void aChangeMadePastApplyIsWrittenAsTheWholeQuest() {
        QuestSync.setPolicy(SHARED_ONCE_STORED);

        try {
            TestQuestProgression quest = quest("Bypassed");
            storage.writeProgressions(List.of(quest));

            quest.forceCounter(7);
            write(storage, quest);

            assertEquals(7, ((TestQuestProgression) storage.loadProgression(quest.getId())).getCounter());
        } finally {
            QuestSync.setPolicy(ALONE);
        }
    }

    @Test
    void aCopyOvertakenByAnotherOfTheSameVersionIsNotTakenForWritten() {
        TestQuestProgression quest = quest("Doubled");
        storage.writeProgressions(List.of(quest));

        // Two passes of one server, each with its own copy of version 1: the older writes first
        TestQuestProgression older = (TestQuestProgression) quest.copy();
        older.setCounter(1);
        quest.setCounter(2);
        assertTrue(storage.writeProgressions(List.of(older)).isEmpty());

        assertEquals(Set.of(quest.getId()), storage.writeProgressions(List.of(quest)));
        assertEquals(1, quest.getStoredVersion());

        // Written over what overtook it, as a quest only this server writes is
        QuestOperation<?> whole = quest.overwrite();
        storage.commitProgression(quest.getId(), stored -> stored.replay(whole));
        assertEquals(2, ((TestQuestProgression) storage.loadProgression(quest.getId())).getCounter());
    }

    @Test
    void aQuestNoLongerSharedForgetsWhatItKept() {
        QuestSync.setPolicy(SHARED_ONCE_STORED);

        try {
            TestQuestProgression quest = quest("Unshared");
            storage.writeProgressions(List.of(quest));
            quest.addToCounter(5);

            QuestSync.setPolicy(ALONE);
            quest.addToCounter(1);

            QuestSync.setPolicy(SHARED_ONCE_STORED);
            write(storage, quest);

            assertEquals(6, ((TestQuestProgression) storage.loadProgression(quest.getId())).getCounter());
        } finally {
            QuestSync.setPolicy(ALONE);
        }
    }

    @Test
    void aVersionGrowsWithEveryWrite() {
        TestQuestProgression quest = quest("Counted");

        storage.writeProgressions(List.of(quest));
        assertEquals(1, quest.getStoredVersion());

        storage.writeProgressions(List.of(quest));
        assertEquals(2, quest.getStoredVersion());

        storage.commitProgression(quest.getId(), stored -> addTo(stored, 1));
        assertEquals(Map.of(quest.getId(), 3L), storage.loadVersions(List.of(quest.getId(), UUID.randomUUID())));
        assertNull(storage.commitProgression(UUID.randomUUID(), stored -> true));
    }

    @Test
    void aQuestDoneAwayWithOnceEndedStaysEndedForTheOthers() {
        String url = "jdbc:h2:mem:openquests-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        QuestStorage onA = open(url);
        QuestStorage onB = open(url);

        try {
            UUID alice = UUID.randomUUID();
            TestQuestProgression quest = quest("Unheld", alice);
            onA.writeProgressions(List.of(quest));
            long seenByB = onB.loadProgression(quest.getId()).getStoredVersion();

            quest.end(QuestState.SUCCESSFUL);
            onA.writeProgressions(List.of(quest));
            onA.deleteProgression(quest);

            // B, still running it, sees it moved and reads the end
            assertTrue(onB.loadVersions(List.of(quest.getId())).get(quest.getId()) > seenByB);
            assertEquals(QuestState.SUCCESSFUL, onB.loadProgression(quest.getId()).getState());
            assertTrue(onB.loadPlayerProgressions(alice).isEmpty());
        } finally {
            onA.close();
            onB.close();
        }
    }

    @Test
    void anOutcomeNobodyNeedsAnyMoreIsDroppedAtStart() throws java.sql.SQLException {
        String url = "jdbc:h2:mem:openquests-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        QuestStorage onA = open(url);

        TestQuestProgression longAgo = quest("LongAgo");
        TestQuestProgression lately = quest("Lately");
        TestQuestProgression held = quest("Held", UUID.randomUUID());
        onA.writeProgressions(List.of(longAgo, lately, held));

        long twoDaysAgo = Instant.now().minus(Duration.ofDays(2)).toEpochMilli();
        try (java.sql.Connection connection = java.sql.DriverManager.getConnection(url);
             java.sql.PreparedStatement ended = connection.prepareStatement("UPDATE oqtest_quest SET state = 'SUCCESSFUL', completed_at = ? WHERE id = ?")) {
            endAt(ended, twoDaysAgo, longAgo);
            endAt(ended, System.currentTimeMillis(), lately);
            endAt(ended, twoDaysAgo, held);
        }
        onA.close();

        QuestStorage onB = open(url);
        try {
            assertEquals(Set.of(lately.getId(), held.getId()), onB.loadVersions(List.of(longAgo.getId(), lately.getId(), held.getId())).keySet());
        } finally {
            onB.close();
        }
    }

    private static void endAt(@Nonnull java.sql.PreparedStatement ended, long at, @Nonnull AbstractQuestProgression<?> quest) throws java.sql.SQLException {
        ended.setLong(1, at);
        ended.setString(2, quest.getId().toString());
        ended.executeUpdate();
    }

    @Test
    void aPlayerMovingInIsReadOnceTheServerTheyLeftWroteThemOut() throws Exception {
        String url = "jdbc:h2:mem:openquests-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        QuestStorage onA = open(url);
        QuestStorage onB = open(url);

        try {
            UUID alice = UUID.randomUUID();
            onA.hostPlayer(alice);
            onA.savePlayer(alice, new PlayerQuestRecord(Set.of(), handed("OnConnection", "Chain"), Set.of(), Map.of(), Map.of()), List.of(), false);

            CompletableFuture<PlayerQuestRecord> readByB = CompletableFuture.supplyAsync(() -> onB.hostPlayer(alice));
            Thread.sleep(500);
            assertFalse(readByB.isDone());

            onA.savePlayer(alice, new PlayerQuestRecord(Set.of(), handed("OnConnection", "Chain", "Other"), Set.of(), Map.of(), Map.of()), List.of(), true);

            assertEquals(handed("OnConnection", "Chain", "Other"), readByB.get(5, TimeUnit.SECONDS).getAssignments());
        } finally {
            onA.close();
            onB.close();
        }
    }

    @Test
    void aServerThatStoppedHoldingAPlayerCannotWriteThemOver() {
        String url = "jdbc:h2:mem:openquests-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        QuestStorage onA = open(url);
        QuestStorage onB = open(url);

        try {
            UUID alice = UUID.randomUUID();
            onA.hostPlayer(alice);

            // A stops renewing: its hold runs out, and B takes Alice without waiting
            onA.renewHosting(Duration.ZERO);
            onB.hostPlayer(alice);
            assertTrue(onB.savePlayer(alice, new PlayerQuestRecord(Set.of(), handed("OnConnection", "Chain", "Other"), Set.of(), Map.of(), Map.of()), List.of(), false));

            assertFalse(onA.savePlayer(alice, new PlayerQuestRecord(Set.of(), handed("OnConnection", "Chain"), Set.of(), Map.of(), Map.of()), List.of(), true));
            assertEquals(handed("OnConnection", "Chain", "Other"), onA.loadPlayer(alice).getAssignments());
        } finally {
            onA.close();
            onB.close();
        }
    }

    @Test
    void twoServersAddingToOneIndexKeepBoth() {
        String url = "jdbc:h2:mem:openquests-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        QuestStorage onA = open(url);
        QuestStorage onB = open(url);

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
        QuestStorage first = open(url);
        first.close();

        try (java.sql.Connection connection = java.sql.DriverManager.getConnection(url);
             java.sql.Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE oqtest_schema_version");
        } catch (java.sql.SQLException e) {
            throw new AssertionError(e);
        }

        org.junit.jupiter.api.Assertions.assertThrows(QuestStorageException.class, () -> open(url));
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

        storage.savePlayer(playerId, record, delivered, true);
        return storage.loadPlayer(playerId);
    }

    /**
     * A shared quest's changes made again on the latest version, the way a server writes one,
     * without the events a test has no server to send.
     */
    private static void write(@Nonnull QuestStorage on, @Nonnull AbstractQuestProgression<?> quest) {
        List<? extends QuestOperation<?>> changes = quest.getPendingOperations();
        AbstractQuestProgression<?> written = on.commitProgression(quest.getId(), stored -> replay(stored, changes));

        assertNotNull(written);
        quest.restack(written, changes.size());
    }

    private static boolean replay(@Nonnull AbstractQuestProgression<?> stored, @Nonnull List<? extends QuestOperation<?>> changes) {
        boolean changed = false;
        for (QuestOperation<?> change : changes) changed |= stored.replay(change);
        return changed;
    }

    private static boolean addTo(@Nonnull AbstractQuestProgression<?> stored, int delta) {
        ((TestQuestProgression) stored).addToCounter(delta);
        return true;
    }

    /**
     * Every stored quest shared, as a server on a shared database holding universe quests sees it.
     */
    private static final QuestSync.Policy SHARED_ONCE_STORED = new QuestSync.Policy() {
        @Override
        public boolean isShared(@Nonnull AbstractQuestProgression<?> quest) {
            return quest.getStoredVersion() > 0;
        }

        @Override
        public void writeSoon(@Nonnull AbstractQuestProgression<?> quest) {}
    };

    private static final QuestSync.Policy ALONE = new QuestSync.Policy() {
        @Override
        public boolean isShared(@Nonnull AbstractQuestProgression<?> quest) {
            return false;
        }

        @Override
        public void writeSoon(@Nonnull AbstractQuestProgression<?> quest) {}
    };

    @Nonnull
    private static QuestStorage open(@Nonnull String url) {
        QuestStorage storage = new JdbcQuestStorage(new JdbcQuestStorage.JdbcSettings(
            url, null, null, null, null, "oqtest_", 4, 10, true, SqlDialect.fromUrl(url)));

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
            "oqtest_", poolSize, 10, true, SqlDialect.fromUrl(url)));

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
        for (UUID playerId : playerIds) quest.join(playerId);

        // The one field a record cannot invent later
        quest.onRegistered();

        return quest;
    }

    @Nonnull
    private static Set<UUID> ids(@Nonnull List<AbstractQuestProgression<?>> quests) {
        return quests.stream().map(AbstractQuestProgression::getId).collect(Collectors.toSet());
    }
}
