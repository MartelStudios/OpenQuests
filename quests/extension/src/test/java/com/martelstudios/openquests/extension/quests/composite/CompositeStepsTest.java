package com.martelstudios.openquests.extension.quests.composite;

import com.martelstudios.openquests.core.models.OpenQuestAsset;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.models.QuestVisibility;
import com.martelstudios.openquests.extension.journal.QuestMark;
import org.junit.jupiter.api.Test;

import java.util.List;
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

    @Test
    void aLockedGroupOnlyNamesWhatIsAlwaysListed() {
        assertEquals(List.of("step0"), previewed(CompositeQuestAsset.Operator.AND, QuestMark.LOCKED));
        assertEquals(List.of("step0"), previewed(CompositeQuestAsset.Operator.AND, null));
    }

    @Test
    void anAndThatSucceededNamesAllButTheNeverStep() {
        assertEquals(List.of("step0", "step1", "step2"), previewed(CompositeQuestAsset.Operator.AND, QuestMark.SUCCESSFUL));
    }

    @Test
    void anOrThatSucceededKeepsSuccessStepsHidden() {
        assertEquals(List.of("step0", "step1"), previewed(CompositeQuestAsset.Operator.OR, QuestMark.SUCCESSFUL));
    }

    @Test
    void aFailedGroupKeepsSuccessStepsHidden() {
        assertEquals(List.of("step0", "step1"), previewed(CompositeQuestAsset.Operator.AND, QuestMark.FAILED));
    }

    private static List<String> previewed(CompositeQuestAsset.Operator operator, QuestMark mark) {
        CompositeScene scene = new CompositeScene(operator)
            .running(QuestVisibility.ALWAYS)
            .running(QuestVisibility.WHEN_COMPLETED)
            .running(QuestVisibility.WHEN_SUCCESSFUL)
            .running(QuestVisibility.NEVER);

        return scene.steps().previewedIn(scene.group.getAsset(), mark).stream().map(OpenQuestAsset::getId).toList();
    }

    private static String tally(CompositeScene scene) {
        return new CompositeQuestPageRenderer(scene.steps()).tallyOf(VIEWER, scene.group);
    }
}
