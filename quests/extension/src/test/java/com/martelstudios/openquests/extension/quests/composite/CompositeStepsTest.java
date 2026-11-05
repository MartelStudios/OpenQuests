package com.martelstudios.openquests.extension.quests.composite;

import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.models.QuestVisibility;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Which steps a list names and counts, whether they are still in the store or gone from it.
 */
class CompositeStepsTest {

    private static final UUID VIEWER = UUID.randomUUID();

    @Test
    void aHiddenBranchStaysOutOfTheTallyWhileItRuns() {
        CompositeScene scene = new CompositeScene(CompositeQuestAsset.Operator.OR)
            .running(QuestVisibility.ALWAYS)
            .running(QuestVisibility.WHEN_SUCCESSFUL);

        assertEquals("0/1", tally(scene));
    }

    @Test
    void aBranchListedOnSuccessShowsOnceItWon() {
        CompositeScene scene = new CompositeScene(CompositeQuestAsset.Operator.OR)
            .gone(QuestVisibility.ALWAYS, QuestState.ABANDONED)
            .gone(QuestVisibility.WHEN_SUCCESSFUL, QuestState.SUCCESSFUL);

        assertEquals("1/2", tally(scene));
    }

    @Test
    void aBranchListedOnSuccessStaysHiddenOnceItLost() {
        CompositeScene scene = new CompositeScene(CompositeQuestAsset.Operator.OR)
            .gone(QuestVisibility.ALWAYS, QuestState.SUCCESSFUL)
            .gone(QuestVisibility.WHEN_SUCCESSFUL, QuestState.ABANDONED);

        assertEquals("1/1", tally(scene));
    }

    @Test
    void aNeverStepIsNotCountedEvenWhenItWins() {
        CompositeScene scene = new CompositeScene(CompositeQuestAsset.Operator.OR)
            .gone(QuestVisibility.ALWAYS, QuestState.ABANDONED)
            .gone(QuestVisibility.NEVER, QuestState.SUCCESSFUL);

        assertEquals("0/1", tally(scene));
    }

    private static String tally(CompositeScene scene) {
        return new CompositeQuestPageRenderer(scene.steps()).tallyOf(VIEWER, scene.group);
    }
}
