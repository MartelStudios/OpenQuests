package com.martelstudios.openquests.extension.quests.queststate;

import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.visitors.QuestVisitor;

import javax.annotation.Nonnull;
import java.util.UUID;

public class QuestStateQuestVisitor implements QuestVisitor<QuestStateQuestProgression> {

    private final UUID playerId;

    public QuestStateQuestVisitor(@Nonnull UUID playerId) {
        this.playerId = playerId;
    }

    /**
     * Read off the player's tally, which counts what ended from their record: a watched quest
     * answers the same way once it is gone from memory, or was never kept at all.
     */
    @Override
    public void progress(QuestStateQuestProgression quest) {
        if (quest.isOver()) return;

        boolean met = quest.getRequirement().isMetBy(quest.environment().services().getQuestPlayerStateService().getTally(playerId, quest.getQuestAssetId()));
        quest.apply(new AbstractQuestProgression.SetState(met ? QuestState.SUCCESSFUL : QuestState.IN_PROGRESS));
    }

    @Override
    public Class<QuestStateQuestProgression> getQuestType() {
        return QuestStateQuestProgression.class;
    }
}
