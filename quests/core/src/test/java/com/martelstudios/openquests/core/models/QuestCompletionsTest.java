package com.martelstudios.openquests.core.models;

import com.martelstudios.openquests.core.persistence.CodecJson;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestCompletionsTest {

    @Test
    void countsEachOutcomeApart() {
        QuestCompletions completions = QuestCompletions.NONE
            .record(QuestState.SUCCESSFUL, null, null)
            .record(QuestState.SUCCESSFUL, null, null)
            .record(QuestState.FAILED, null, null)
            .record(QuestState.ABANDONED, null, null);

        assertEquals(2, completions.count(QuestState.SUCCESSFUL));
        assertEquals(1, completions.count(QuestState.FAILED));
        assertEquals(1, completions.count(QuestState.ABANDONED));
        assertEquals(4, completions.total());
    }

    @Test
    void aQuestStillRunningIsNotAnOutcome() {
        assertSame(QuestCompletions.NONE, QuestCompletions.NONE.record(QuestState.IN_PROGRESS, Instant.EPOCH, Instant.EPOCH));
        assertEquals(0, QuestCompletions.NONE.count(QuestState.IN_PROGRESS));
    }

    @Test
    void countingReturnsANewInstanceAndLeavesTheOldOneAlone() {
        QuestCompletions once = QuestCompletions.NONE.record(QuestState.SUCCESSFUL, null, null);

        once.record(QuestState.SUCCESSFUL, null, null);

        assertEquals(1, once.count(QuestState.SUCCESSFUL));
        assertEquals(0, QuestCompletions.NONE.total());
    }

    @Test
    void anOlderQuestHeardOfLateDoesNotMoveTheDatesBack() {
        QuestCompletions completions = QuestCompletions.NONE
            .record(QuestState.SUCCESSFUL, Instant.ofEpochMilli(3_000), Instant.ofEpochMilli(4_000))
            .record(QuestState.FAILED, Instant.ofEpochMilli(1_000), Instant.ofEpochMilli(2_000));

        assertEquals(Instant.ofEpochMilli(3_000), completions.getLastStartedAt());
        assertEquals(Instant.ofEpochMilli(4_000), completions.getLastCompletedAt());
    }

    @Test
    void writesOnlyWhatHappened() {
        QuestCompletions completions = QuestCompletions.NONE.record(QuestState.FAILED, null, Instant.ofEpochMilli(2_000));

        String json = CodecJson.encode(QuestCompletions.CODEC, completions);

        assertTrue(json.contains("Failed"));
        assertTrue(json.contains("LastCompletedAt"));
        assertFalse(json.contains("Successful"));
        assertFalse(json.contains("LastStartedAt"));

        QuestCompletions read = CodecJson.decode(QuestCompletions.CODEC, json, "completions");
        assertEquals(1, read.count(QuestState.FAILED));
        assertEquals(0, read.count(QuestState.SUCCESSFUL));
        assertNull(read.getLastStartedAt());
        assertEquals(Instant.ofEpochMilli(2_000), read.getLastCompletedAt());
    }
}
