package com.martelstudios.openquests.extension.quests.breakblock;

import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.visitors.QuestVisitor;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Counts one broken block against the quests targeting it. The block is read off the event rather
 * than off what the player is holding: what was broken is a fact, what it was broken with is not
 * this quest's business.
 */
public class BreakBlockQuestVisitor implements QuestVisitor<BreakBlockQuestProgression> {

    private final UUID playerId;
    private final BreakBlockEvent event;

    public BreakBlockQuestVisitor(@Nonnull UUID playerId, @Nonnull BreakBlockEvent event) {
        this.playerId = playerId;
        this.event = event;
    }

    @Override
    public void progress(BreakBlockQuestProgression quest) {
        if (!quest.getPlayers().contains(playerId)) return;
        if (quest.isCompleted() && quest.isStopOnComplete()) return;

        BlockType blockType = event.getBlockType();
        if (!quest.getBlockToBreak().isBlockTypeIncluded(blockType.getId())) return;

        quest.setCurrentQuantity(quest.getCurrentQuantity() + 1)
             .setState(quest.checkCompletion() ? QuestState.SUCCESSFUL : QuestState.IN_PROGRESS)
             .markDirty();
    }

    @Override
    public Class<BreakBlockQuestProgression> getQuestType() {
        return BreakBlockQuestProgression.class;
    }
}
