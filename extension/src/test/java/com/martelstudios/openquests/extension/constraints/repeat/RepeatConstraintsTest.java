package com.martelstudios.openquests.extension.constraints.repeat;

import com.martelstudios.openquests.core.models.QuestCompletions;
import com.martelstudios.openquests.core.models.QuestState;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static com.martelstudios.openquests.extension.constraints.ConstraintFixtures.asset;
import static com.martelstudios.openquests.extension.constraints.ConstraintFixtures.request;
import static com.martelstudios.openquests.extension.constraints.ConstraintFixtures.tally;
import static com.martelstudios.openquests.extension.constraints.ConstraintFixtures.decode;
import static com.martelstudios.openquests.extension.constraints.ConstraintFixtures.tryDecode;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepeatConstraintsTest {

    private static final UUID PLAYER = UUID.randomUUID();
    private static final Instant STARTED = Instant.parse("2026-09-26T08:00:00Z");
    private static final Instant ENDED = Instant.parse("2026-09-26T09:00:00Z");

    @Test
    void aPlayerWhoNeverEndedOneMayTakeItRightAway() {
        CooldownConstraint cooldown = decode(CooldownConstraint.CODEC, "{\"Seconds\": 86400}");

        assertNull(cooldown.getAvailableAt(tally(QuestCompletions.NONE)));
        assertTrue(cooldown.allowsAssignment(request(QuestCompletions.NONE, STARTED)));
    }

    @Test
    void theWaitRunsFromTheEndOfTheLastOneByDefault() {
        CooldownConstraint cooldown = decode(CooldownConstraint.CODEC, "{\"Seconds\": 86400}");
        QuestCompletions once = QuestCompletions.NONE.record(QuestState.SUCCESSFUL, STARTED, ENDED);

        assertEquals(Duration.ofDays(1), cooldown.getDuration());
        assertEquals(ENDED.plus(Duration.ofDays(1)), cooldown.getAvailableAt(tally(once)));
        assertFalse(cooldown.allowsAssignment(request(once, ENDED.plus(Duration.ofHours(23)))));
        assertTrue(cooldown.allowsAssignment(request(once, ENDED.plus(Duration.ofDays(1)))));
    }

    @Test
    void givingUpDoesNotSkipTheWait() {
        CooldownConstraint cooldown = decode(CooldownConstraint.CODEC, "{\"Seconds\": 3600}");
        QuestCompletions abandoned = QuestCompletions.NONE.record(QuestState.ABANDONED, STARTED, ENDED);

        assertFalse(cooldown.allowsAssignment(request(abandoned, ENDED.plus(Duration.ofMinutes(30)))));
    }

    @Test
    void theWaitCanRunFromTheStartInstead() {
        CooldownConstraint cooldown = decode(CooldownConstraint.CODEC, "{\"Seconds\": 86400, \"From\": \"Start\"}");
        QuestCompletions once = QuestCompletions.NONE.record(QuestState.SUCCESSFUL, STARTED, ENDED);

        assertEquals(STARTED.plus(Duration.ofDays(1)), cooldown.getAvailableAt(tally(once)));
    }

    @Test
    void aCooldownOfNothingDoesNotDecode() {
        assertNull(tryDecode(CooldownConstraint.CODEC, "{\"Seconds\": 0}"));
    }

    @Test
    void onlySuccessesUseUpATryByDefault() {
        MaxCompletionsConstraint max = decode(MaxCompletionsConstraint.CODEC, "{\"Count\": 2}");
        QuestCompletions completions = QuestCompletions.NONE
            .record(QuestState.SUCCESSFUL, null, null)
            .record(QuestState.FAILED, null, null)
            .record(QuestState.ABANDONED, null, null);

        assertEquals(1, max.getRemaining(tally(completions)));
        assertTrue(max.allowsAssignment(request(completions, ENDED)));

        QuestCompletions twice = completions.record(QuestState.SUCCESSFUL, null, null);
        assertEquals(0, max.getRemaining(tally(twice)));
        assertFalse(max.allowsAssignment(request(twice, ENDED)));
    }

    @Test
    void theAssetCanCountOtherOutcomesToo() {
        MaxCompletionsConstraint max = decode(MaxCompletionsConstraint.CODEC, "{\"Count\": 2, \"Outcomes\": [\"Successful\", \"Failed\", \"Abandoned\"]}");
        QuestCompletions completions = QuestCompletions.NONE
            .record(QuestState.FAILED, null, null)
            .record(QuestState.ABANDONED, null, null);

        assertEquals(0, max.getRemaining(tally(completions)));
        assertFalse(max.allowsAssignment(request(completions, ENDED)));
    }

    @Test
    void countingQuestsStillRunningIsRefusedAtBoot() {
        MaxCompletionsConstraint max = decode(MaxCompletionsConstraint.CODEC, "{\"Count\": 1, \"Outcomes\": [\"InProgress\"]}");

        assertNotNull(max.validate(asset()));
        assertNull(decode(MaxCompletionsConstraint.CODEC, "{\"Count\": 1}").validate(asset()));
    }

    @Test
    void allowingNothingDoesNotDecode() {
        assertNull(tryDecode(MaxCompletionsConstraint.CODEC, "{\"Count\": 0}"));
    }
}
