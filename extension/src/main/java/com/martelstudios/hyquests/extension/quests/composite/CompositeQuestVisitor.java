package com.martelstudios.hyquests.extension.quests.composite;

import com.martelstudios.hyquests.core.models.QuestState;
import com.martelstudios.hyquests.core.visitors.QuestVisitor;

/**
 * Succeeds a {@link CompositeQuest} once all of its child quests have completed. Carries no
 * context: the quest's progress is entirely derived from its children.
 */
public class CompositeQuestVisitor implements QuestVisitor<CompositeQuest> {

    @Override
    public void progress(CompositeQuest quest) {
        if (quest.isCompleted()) return;

        if (quest.allQuestsCompleted()) {
            quest.setState(QuestState.SUCCESSFUL).markDirty();
        }
    }

    @Override
    public Class<CompositeQuest> getQuestType() {
        return CompositeQuest.class;
    }
}
