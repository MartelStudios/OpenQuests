package com.martelstudios.openquests.extension.quests.kill.npc;

import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.visitors.QuestVisitor;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Counts one kill against the killer's quests whose group the victim belongs to.
 */
public class KillNpcQuestVisitor implements QuestVisitor<KillNpcQuestProgression> {

    private final UUID killerId;
    private final NPCEntity victim;

    public KillNpcQuestVisitor(@Nonnull UUID killerId, @Nonnull NPCEntity victim) {
        this.killerId = killerId;
        this.victim = victim;
    }

    @Override
    public void progress(KillNpcQuestProgression quest) {
        if (!quest.getPlayers().contains(killerId)) return;
        if (quest.isCompleted()) return;
        if (!quest.matchesVictim(victim)) return;

        quest.setCurrentQuantity(quest.getCurrentQuantity() + 1)
             .setState(quest.checkCompletion() ? QuestState.SUCCESSFUL : QuestState.IN_PROGRESS)
             .markDirty();
    }

    @Override
    public Class<KillNpcQuestProgression> getQuestType() {
        return KillNpcQuestProgression.class;
    }
}
