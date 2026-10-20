package com.martelstudios.openquests.core;

import com.martelstudios.openquests.core.constraints.QuestConstraint;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.persistence.TestQuestProgression;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A quest whose constraint gives it a deadline ends on its own when the deadline comes, on the
 * outcome the constraint names, and not before.
 */
class QuestDeadlineTest {

    private final QuestHarness harness = new QuestHarness();

    @AfterEach
    void close() {
        harness.close();
    }

    @Test
    void aQuestEndsWhenItsDeadlineComes() {
        TestQuestProgression quest = harness.register(new TestQuestAsset(new EndsAfter(Duration.ofSeconds(10), QuestState.FAILED)));

        harness.clock().advance(Duration.ofSeconds(10));
        harness.scheduler().runDue();

        assertEquals(QuestState.FAILED, quest.getState());
    }

    @Test
    void aQuestRunsOnUntilItsDeadline() {
        TestQuestProgression quest = harness.register(new TestQuestAsset(new EndsAfter(Duration.ofSeconds(10), QuestState.FAILED)));

        harness.clock().advance(Duration.ofSeconds(9));
        assertEquals(0, harness.scheduler().runDue());

        assertEquals(QuestState.IN_PROGRESS, quest.getState());
    }

    @Test
    void theEarliestDeadlineDecidesTheOutcome() {
        TestQuestProgression quest = harness.register(new TestQuestAsset(
            new EndsAfter(Duration.ofSeconds(20), QuestState.FAILED),
            new EndsAfter(Duration.ofSeconds(5), QuestState.SUCCESSFUL)));

        harness.clock().advance(Duration.ofSeconds(5));
        harness.scheduler().runDue();

        assertEquals(QuestState.SUCCESSFUL, quest.getState());
    }

    @Test
    void aQuestOverBeforeItsDeadlineIsLeftAsItEnded() {
        TestQuestProgression quest = harness.register(new TestQuestAsset(new EndsAfter(Duration.ofSeconds(10), QuestState.FAILED)));
        harness.count(quest, 5, 5);

        harness.clock().advance(Duration.ofSeconds(10));
        harness.scheduler().runDue();

        assertEquals(QuestState.SUCCESSFUL, quest.getState());
    }

    /**
     * Ends the quest a while after it started.
     */
    private static final class EndsAfter extends QuestConstraint {
        private final Duration after;
        private final QuestState outcome;

        EndsAfter(@Nonnull Duration after, @Nonnull QuestState outcome) {
            this.after = after;
            this.outcome = outcome;
        }

        @Override
        public Instant getDeadline(@Nonnull AbstractQuestProgression<?> quest) {
            return quest.getStartedAt() == null ? null : quest.getStartedAt().plus(after);
        }

        @Nonnull
        @Override
        public QuestState getExpiredState() {
            return outcome;
        }
    }
}
