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
     * The outcome the child announced, rather than whatever it carries by the time this runs.
     */
    @Nonnull
    private final QuestState outcome;

    public CompositeQuestVisitor(@Nonnull AbstractQuestProgression<?> updatedChild, @Nonnull QuestState outcome) {
        this.updatedChild = updatedChild;
        this.outcome = outcome;
    }

    @Override
    public void progress(CompositeQuestProgression quest) {
        if (quest.isCompleted() && quest.isStopOnComplete()) return;

        if (quest.recordOutcome(updatedChild.getId(), outcome)) quest.markDirty();

        int children = quest.getChildIds().length;
        int successful = quest.countOutcomes(QuestState.SUCCESSFUL);
        int failed = quest.countOutcomes(QuestState.FAILED);
        int abandoned = quest.countOutcomes(QuestState.ABANDONED);

        switch (quest.getAsset().getOperator()) {
            case AND -> {
                if (abandoned > 0) {
                    quest.setState(QuestState.ABANDONED).markDirty();
                } else if (failed > 0) {
                    quest.setState(QuestState.FAILED).markDirty();
                } else if (successful >= children) {
                    quest.setState(QuestState.SUCCESSFUL).markDirty();
                }
            }
            case OR -> {
                if (successful > 0) {
                    quest.setState(QuestState.SUCCESSFUL).markDirty();
                } else if (failed + abandoned >= children) {
                    // A composite quest to be ABANDONED has to have all its subquest abandoned.
                    quest.setState(failed == 0 ? QuestState.ABANDONED : QuestState.FAILED).markDirty();
                }
            }
        }
    }

    @Override
    public Class<CompositeQuestProgression> getQuestType() {
        return CompositeQuestProgression.class;
    }
}
