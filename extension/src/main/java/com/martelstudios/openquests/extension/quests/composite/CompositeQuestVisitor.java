package com.martelstudios.openquests.extension.quests.composite;

import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.visitors.QuestVisitor;

import javax.annotation.Nonnull;

/**
 * Settles a {@link CompositeQuestProgression} from its children's outcomes.
 */
public class CompositeQuestVisitor implements QuestVisitor<CompositeQuestProgression> {

    @Nonnull
    private final AbstractQuestProgression<?> updatedChild;

    /**
     * The state the child moved to, rather than whatever it carries by the time this runs.
     */
    @Nonnull
    private final QuestState outcome;

    public CompositeQuestVisitor(@Nonnull AbstractQuestProgression<?> updatedChild, @Nonnull QuestState outcome) {
        this.updatedChild = updatedChild;
        this.outcome = outcome;
    }

    @Override
    public void progress(CompositeQuestProgression quest) {
        if (quest.isOver()) return;

        if (quest.recordOutcome(updatedChild.getId(), outcome)) quest.markDirty();

        int children = quest.getChildIds().length;
        int successful = quest.countOutcomes(QuestState.SUCCESSFUL);
        int failed = quest.countOutcomes(QuestState.FAILED);
        int abandoned = quest.countOutcomes(QuestState.ABANDONED);

        // Weighed whole every time: a group kept alive by StopOnComplete:false whose rule no longer
        // holds, a step having gone back to running, goes back to running too
        QuestState target = switch (quest.getAsset().getOperator()) {
            case AND -> abandoned > 0 ? QuestState.ABANDONED
                      : failed > 0 ? QuestState.FAILED
                      : successful >= children ? QuestState.SUCCESSFUL
                      : QuestState.IN_PROGRESS;
            // A composite quest to be ABANDONED has to have all its subquest abandoned.
            case OR -> successful > 0 ? QuestState.SUCCESSFUL
                     : failed + abandoned >= children ? (failed == 0 ? QuestState.ABANDONED : QuestState.FAILED)
                     : QuestState.IN_PROGRESS;
        };

        if (target != quest.getState()) quest.setState(target).markDirty();
    }

    @Override
    public Class<CompositeQuestProgression> getQuestType() {
        return CompositeQuestProgression.class;
    }
}
