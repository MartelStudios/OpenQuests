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

    public CompositeQuestVisitor(@Nonnull AbstractQuestProgression<?> updatedChild) {
        this.updatedChild = updatedChild;
    }

    @Override
    public void progress(CompositeQuestProgression quest) {
        if (quest.isCompleted() && quest.isStopOnComplete()) return;

        var childId = updatedChild.getId();

        if (updatedChild.isSuccessful() && quest.successfulQuestIds.add(childId)) {
            quest.failedQuestIds.remove(childId);
            quest.abandonedQuestIds.remove(childId);
            quest.markDirty();
        } else if (updatedChild.isFailed() && quest.failedQuestIds.add(childId)) {
            quest.successfulQuestIds.remove(childId);
            quest.abandonedQuestIds.remove(childId);
            quest.markDirty();
        } else if (updatedChild.isAbandoned() && quest.abandonedQuestIds.add(childId)) {
            quest.failedQuestIds.remove(childId);
            quest.successfulQuestIds.remove(childId);
            quest.markDirty();
        }

        switch (quest.getAsset().getOperator()) {
            case AND -> {
                if (!quest.abandonedQuestIds.isEmpty()) {
                    quest.setState(QuestState.ABANDONED).markDirty();
                } else if (!quest.failedQuestIds.isEmpty()) {
                    quest.setState(QuestState.FAILED).markDirty();
                } else if (quest.successfulQuestIds.size() >= quest.questIds.length) {
                    quest.setState(QuestState.SUCCESSFUL).markDirty();
                }
            }
            case OR -> {
                if (!quest.successfulQuestIds.isEmpty()) {
                    quest.setState(QuestState.SUCCESSFUL).markDirty();
                } else if (quest.failedQuestIds.size() + quest.abandonedQuestIds.size() >= quest.questIds.length) {
                    // A composite quest to be ABANDONED has to have all its subquest abandoned.
                    quest.setState(quest.failedQuestIds.isEmpty() ? QuestState.ABANDONED : QuestState.FAILED)
                         .markDirty();
                }
            }
        }
    }

    @Override
    public Class<CompositeQuestProgression> getQuestType() {
        return CompositeQuestProgression.class;
    }
}
