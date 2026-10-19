package com.martelstudios.openquests.extension.constraints.repeat;

import com.martelstudios.openquests.core.models.QuestCompletions;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.models.QuestTally;
import com.martelstudios.openquests.extension.constraints.ConstraintFixtures.TestQuest;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.martelstudios.openquests.extension.constraints.ConstraintFixtures.asset;
import static com.martelstudios.openquests.extension.constraints.ConstraintFixtures.request;
import static com.martelstudios.openquests.extension.constraints.ConstraintFixtures.tally;
import static com.martelstudios.openquests.extension.constraints.ConstraintFixtures.decode;
import static com.martelstudios.openquests.extension.constraints.ConstraintFixtures.questStartedAt;
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
    void everyStateCountsByDefault() {
        MaxQuestsConstraint max = decode(MaxQuestsConstraint.CODEC, "{\"Count\": 3}");
        QuestTally tally = withRunning(QuestCompletions.NONE.record(QuestState.FAILED, null, null).record(QuestState.ABANDONED, null, null), 1);

        assertEquals(0, max.getRemaining(tally));
        assertFalse(max.allowsAssignment(request(tally, ENDED)));
    }

    @Test
    void successesAloneMakeANumberOfWins() {
        MaxQuestsConstraint max = decode(MaxQuestsConstraint.CODEC, "{\"Count\": 2, \"States\": [\"Successful\"]}");
        QuestCompletions completions = QuestCompletions.NONE
            .record(QuestState.SUCCESSFUL, null, null)
            .record(QuestState.FAILED, null, null)
            .record(QuestState.ABANDONED, null, null);

        assertEquals(1, max.getRemaining(withRunning(completions, 1)));
        assertFalse(max.allowsAssignment(request(completions.record(QuestState.SUCCESSFUL, null, null), ENDED)));
    }

    @Test
    void runningQuestsAloneMakeOneAtATime() {
        MaxQuestsConstraint max = decode(MaxQuestsConstraint.CODEC, "{\"Count\": 1, \"States\": [\"InProgress\"]}");
        QuestCompletions done = QuestCompletions.NONE.record(QuestState.SUCCESSFUL, null, null);

        assertTrue(max.isAtOnce());
        assertTrue(max.allowsAssignment(request(withRunning(done, 0), ENDED)));
        assertFalse(max.allowsAssignment(request(withRunning(done, 1), ENDED)));
    }

    @Test
    void runningOrWonKeepsAQuestFromRunningTwiceOrComingBack() {
        MaxQuestsConstraint max = decode(MaxQuestsConstraint.CODEC, "{\"Count\": 1, \"States\": [\"InProgress\", \"Successful\"]}");
        QuestCompletions failed = QuestCompletions.NONE.record(QuestState.FAILED, null, null);

        assertTrue(max.allowsAssignment(request(withRunning(failed, 0), ENDED)));
        assertFalse(max.allowsAssignment(request(withRunning(failed, 1), ENDED)));
        assertFalse(max.allowsAssignment(request(failed.record(QuestState.SUCCESSFUL, null, null), ENDED)));
    }

    @Test
    void allowingNothingOrCountingNothingDoesNotDecode() {
        assertNull(tryDecode(MaxQuestsConstraint.CODEC, "{\"Count\": 0}"));
        assertNull(tryDecode(MaxQuestsConstraint.CODEC, "{\"Count\": 1, \"States\": []}"));
    }

    /**
     * @return the record standing, with that many quests from the asset running for the player.
     */
    private static QuestTally withRunning(QuestCompletions completions, int running) {
        UUID player = UUID.randomUUID();
        List<TestQuest> quests = new ArrayList<>();
        for (int i = 0; i < running; i++) quests.add(questStartedAt(null).join(player));
        return QuestTally.of(completions, player, quests);
    }
}
