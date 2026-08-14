package com.martelstudios.hyquests.extension.general;

import com.martelstudios.hyquests.core.models.QuestState;
import com.martelstudios.hyquests.core.visitors.QuestVisitor;

/**
 * Succeeds a {@link GeneralQuest} once all of its child quests have completed. Carries no
 * context: the quest's progress is entirely derived from its children.
 */
public class GeneralQuestVisitor implements QuestVisitor<GeneralQuest> {

    @Override
    public void progress(GeneralQuest quest) {
        if (quest.isCompleted()) return;

        if (quest.allQuestsCompleted()) {
            quest.setState(QuestState.SUCCESSFUL).markDirty();
        }
    }

    @Override
    public Class<GeneralQuest> getQuestType() {
        return GeneralQuest.class;
    }
}
