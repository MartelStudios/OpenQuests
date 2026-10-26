package com.martelstudios.openquests.extension.quests.useblock;

import com.hypixel.hytale.server.core.event.events.ecs.UseBlockEvent;
import com.martelstudios.openquests.core.visitors.QuestVisitor;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestProgression;

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
        if (quest.isOver()) return;

        var blockType = event.getBlockType();
        if (blockType == null || !quest.getBlockToUse().matches(blockType.getId())) return;

        quest.apply(new QuantityQuestProgression.Add(1));
    }

    @Override
    public Class<UseBlockQuestProgression> getQuestType() {
        return UseBlockQuestProgression.class;
    }

    @Nonnull
    @Override
    public UUID getActorId() {
        return playerId;
    }
}
