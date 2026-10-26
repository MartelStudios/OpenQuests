package com.martelstudios.openquests.core.models;

import com.martelstudios.openquests.core.persistence.TestQuestProgression;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestVisibilityTest {

    @Test
    void onlyAlwaysShowsAQuestNeverHandedOut() {
        assertTrue(QuestVisibility.ALWAYS.isReachedBy(null));
        assertFalse(QuestVisibility.WHEN_STARTED.isReachedBy(null));
        assertFalse(QuestVisibility.WHEN_SUCCESSFUL.isReachedBy(null));
    }

    @Test
    void aRunningQuestIsStartedButNotYetCompleted() {
        TestQuestProgression running = new TestQuestProgression();

        assertTrue(QuestVisibility.WHEN_STARTED.isReachedBy(running));
        assertFalse(QuestVisibility.WHEN_COMPLETED.isReachedBy(running));
        assertFalse(QuestVisibility.NEVER.isReachedBy(running));
    }

    @Test
    void aFailedQuestIsCompletedButNotSuccessful() {
        TestQuestProgression failed = new TestQuestProgression().end(QuestState.FAILED);
        TestQuestProgression succeeded = new TestQuestProgression().end(QuestState.SUCCESSFUL);

        assertTrue(QuestVisibility.WHEN_COMPLETED.isReachedBy(failed));
        assertFalse(QuestVisibility.WHEN_SUCCESSFUL.isReachedBy(failed));
        assertTrue(QuestVisibility.WHEN_SUCCESSFUL.isReachedBy(succeeded));
    }
}
