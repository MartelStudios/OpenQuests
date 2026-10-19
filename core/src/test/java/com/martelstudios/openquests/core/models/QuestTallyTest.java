package com.martelstudios.openquests.core.models;

import com.martelstudios.openquests.core.persistence.TestQuestProgression;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class QuestTallyTest {

    private final UUID player = UUID.randomUUID();
    private final UUID other = UUID.randomUUID();

    @Test
    void countsRunningQuestsBesideTheRecord() {
        QuestCompletions record = QuestCompletions.NONE.record(QuestState.SUCCESSFUL, null, null);

        QuestTally tally = QuestTally.of(record, player, List.of(quest(player), quest(player)));

        assertEquals(2, tally.count(QuestState.IN_PROGRESS));
        assertEquals(1, tally.count(QuestState.SUCCESSFUL));
        assertEquals(3, tally.count(EnumSet.of(QuestState.IN_PROGRESS, QuestState.SUCCESSFUL)));
    }

    @Test
    void anEndedQuestIsLeftToTheRecordSoItCountsOnce() {
        QuestCompletions record = QuestCompletions.NONE.record(QuestState.SUCCESSFUL, null, null);

        QuestTally tally = QuestTally.of(record, player, List.of(quest(player).end(QuestState.SUCCESSFUL)));

        assertEquals(0, tally.count(QuestState.IN_PROGRESS));
        assertEquals(1, tally.count(QuestState.SUCCESSFUL));
    }

    @Test
    void aSharedQuestGivenUpIsAbandonedForThatPlayerWhileItRunsOn() {
        TestQuestProgression shared = quest(player, other).giveUp(player);

        QuestTally tally = QuestTally.of(QuestCompletions.NONE, player, List.of(shared));

        assertEquals(0, tally.count(QuestState.IN_PROGRESS));
        assertEquals(1, tally.count(QuestState.ABANDONED));
        assertEquals(1, QuestTally.of(QuestCompletions.NONE, other, List.of(shared)).count(QuestState.IN_PROGRESS));
    }

    @Test
    void someoneElsesQuestIsNotCounted() {
        assertSame(QuestTally.NONE, QuestTally.of(QuestCompletions.NONE, player, List.of(quest(other))));
    }

    @Test
    void aRunningQuestCanBeTheLastOneStarted() {
        QuestCompletions record = QuestCompletions.NONE.record(QuestState.FAILED, Instant.ofEpochMilli(1_000), Instant.ofEpochMilli(2_000));
        TestQuestProgression running = quest(player);

        QuestTally tally = QuestTally.of(record, player, List.of(running));

        assertEquals(running.getStartedAt(), tally.getLastStartedAt());
        assertEquals(Instant.ofEpochMilli(2_000), tally.getLastCompletedAt());
    }

    private static TestQuestProgression quest(UUID... players) {
        TestQuestProgression quest = AbstractQuestProgression.Builder.of(new TestQuestProgression(), "Asset").build();
        for (UUID playerId : players) quest.join(playerId);
        quest.onRegistered();
        return quest;
    }
}
