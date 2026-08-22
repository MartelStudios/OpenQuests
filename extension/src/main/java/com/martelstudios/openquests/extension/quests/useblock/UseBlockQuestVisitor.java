package com.martelstudios.openquests.extension.quests.useblock;

import com.hypixel.hytale.server.core.event.events.ecs.UseBlockEvent;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.visitors.QuestVisitor;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Counts one interaction against the quests targeting the used block.
 */
public class UseBlockQuestVisitor implements QuestVisitor<UseBlockQuestProgression> {

    private final UUID playerId;
    private final UseBlockEvent.Post event;

    public UseBlockQuestVisitor(@Nonnull UUID playerId, @Nonnull UseBlockEvent.Post event) {
        this.playerId = playerId;
        this.event = event;
    }

    @Override
    public void progress(UseBlockQuestProgression quest) {
        if (!quest.getPlayers().contains(playerId)) return;
        if (quest.isCompleted() && quest.isStopOnComplete()) return;

        var blockType = event.getBlockType();
        if (blockType == null || !quest.getBlockToUse().isBlockTypeIncluded(blockType.getId())) return;

        quest.setCurrentQuantity(quest.getCurrentQuantity() + 1)
             .setState(quest.checkCompletion() ? QuestState.SUCCESSFUL : QuestState.IN_PROGRESS)
             .markDirty();
    }

    @Override
    public Class<UseBlockQuestProgression> getQuestType() {
        return UseBlockQuestProgression.class;
    }
}
