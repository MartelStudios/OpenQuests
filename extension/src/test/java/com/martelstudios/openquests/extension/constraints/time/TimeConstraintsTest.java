package com.martelstudios.openquests.extension.constraints.time;

import com.martelstudios.openquests.core.models.QuestCompletions;
import com.martelstudios.openquests.core.models.QuestState;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static com.martelstudios.openquests.extension.constraints.ConstraintFixtures.asset;
import static com.martelstudios.openquests.extension.constraints.ConstraintFixtures.decode;
import static com.martelstudios.openquests.extension.constraints.ConstraintFixtures.questStartedAt;
import static com.martelstudios.openquests.extension.constraints.ConstraintFixtures.tryDecode;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeConstraintsTest {

    private static final Instant START = Instant.parse("2026-09-26T12:00:00Z");

    @Test
    void aTimeLimitRunsFromTheStartOfTheQuest() {
        TimeLimitConstraint constraint = decode(TimeLimitConstraint.CODEC, "{\"Seconds\": 300}");

        assertEquals(Duration.ofMinutes(5), constraint.getDuration());
        assertEquals(START.plus(Duration.ofMinutes(5)), constraint.getDeadline(questStartedAt(START)));
    }

    @Test
    void aQuestThatNeverSaidWhenItStartedIsLeftUntimed() {
        TimeLimitConstraint constraint = decode(TimeLimitConstraint.CODEC, "{\"Seconds\": 300}");

        assertNull(constraint.getDeadline(questStartedAt(null)));
    }

    @Test
    void runningOutFailsUnlessTheAssetSaysOtherwise() {
        assertEquals(QuestState.FAILED, decode(TimeLimitConstraint.CODEC, "{\"Seconds\": 60}").getExpiredState());
        assertEquals(QuestState.SUCCESSFUL, decode(TimeLimitConstraint.CODEC, "{\"Seconds\": 60, \"OnExpire\": \"Successful\"}").getExpiredState());
    }

    @Test
    void runningOutIntoAQuestStillRunningIsRefusedAtBoot() {
        TimeLimitConstraint constraint = decode(TimeLimitConstraint.CODEC, "{\"Seconds\": 60, \"OnExpire\": \"InProgress\"}");

        assertNotNull(constraint.validate(asset()));
        assertNull(decode(TimeLimitConstraint.CODEC, "{\"Seconds\": 60}").validate(asset()));
    }

    @Test
    void aTimeLimitOfNothingDoesNotDecode() {
        assertNull(tryDecode(TimeLimitConstraint.CODEC, "{\"Seconds\": 0}"));
    }

    @Test
    void aDeadlineEndsEveryQuestAtTheSameMoment() {
        DeadlineConstraint constraint = decode(DeadlineConstraint.CODEC, "{\"At\": \"2026-12-31T23:00:00Z\"}");
        Instant at = Instant.parse("2026-12-31T23:00:00Z");

        assertEquals(at, constraint.getDeadline(questStartedAt(START)));
        assertEquals(at, constraint.getDeadline(questStartedAt(null)));
    }

    @Test
    void nothingIsHandedOutOnceTheDeadlineHasPassed() {
        DeadlineConstraint constraint = decode(DeadlineConstraint.CODEC, "{\"At\": \"2026-12-31T23:00:00Z\"}");
        UUID playerId = UUID.randomUUID();

        assertTrue(constraint.allowsAssignment(asset(), playerId, QuestCompletions.NONE, Instant.parse("2026-12-31T22:59:59Z")));
        assertFalse(constraint.allowsAssignment(asset(), playerId, QuestCompletions.NONE, Instant.parse("2026-12-31T23:00:00Z")));
    }
}
