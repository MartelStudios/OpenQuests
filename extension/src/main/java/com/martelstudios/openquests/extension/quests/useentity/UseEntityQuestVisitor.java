package com.martelstudios.openquests.extension.quests.useentity;

import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.visitors.QuestVisitor;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Counts one interaction against the quests whose group the used entity belongs to.
 */
public class UseEntityQuestVisitor implements QuestVisitor<UseEntityQuestProgression> {

    private final UUID playerId;
    private final NPCEntity target;

    public UseEntityQuestVisitor(@Nonnull UUID playerId, @Nonnull NPCEntity target) {
        this.playerId = playerId;
        this.target = target;
    }

    @Override
    public void progress(UseEntityQuestProgression quest) {
        if (!quest.getPlayers().contains(playerId)) return;
        if (quest.isCompleted() && quest.isStopOnComplete()) return;
        if (!quest.matchesTarget(target)) return;

        quest.setCurrentQuantity(quest.getCurrentQuantity() + 1)
             .setState(quest.checkCompletion() ? QuestState.SUCCESSFUL : QuestState.IN_PROGRESS)
             .markDirty();
    }

    @Override
    public Class<UseEntityQuestProgression> getQuestType() {
        return UseEntityQuestProgression.class;
    }
}
