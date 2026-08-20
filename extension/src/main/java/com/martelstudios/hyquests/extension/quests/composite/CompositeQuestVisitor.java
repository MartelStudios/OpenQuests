package com.martelstudios.hyquests.extension.quests.composite;

import com.martelstudios.hyquests.core.models.QuestState;
import com.martelstudios.hyquests.core.visitors.QuestVisitor;

/**
 * Settles a {@link CompositeQuestProgression} from its children's outcomes.
 */
public class CompositeQuestVisitor implements QuestVisitor<CompositeQuestProgression> {

    @Override
    public void progress(CompositeQuestProgression quest) {
        if (quest.isCompleted()) return;

        if (quest.isOperatorSatisfied()) {
            quest.setState(QuestState.SUCCESSFUL).markDirty();
        } else if (quest.allQuestsCompleted()) {
            quest.setState(QuestState.FAILED).markDirty();
        }
    }

    @Override
    public Class<CompositeQuestProgression> getQuestType() {
        return CompositeQuestProgression.class;
    }
}
