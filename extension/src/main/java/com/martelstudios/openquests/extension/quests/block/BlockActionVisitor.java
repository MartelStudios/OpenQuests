package com.martelstudios.openquests.extension.quests.block;

import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.visitors.QuestVisitor;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Carries one block placed or broken to every quest counting blocks. Typed on the base so that the
 * place and the break quests are answered in a single pass over the player's quests.
 */
public class BlockActionVisitor implements QuestVisitor<BlockActionQuestProgression<?>> {

    private final UUID playerId;
    private final BlockAction action;
    private final String blockId;
    private final boolean placedByPlayer;

    /**
     * @param blockId        the block's id, or the id of the item placing it, which is the same.
     * @param placedByPlayer for a break, whether a player had placed the block.
     */
    public BlockActionVisitor(@Nonnull UUID playerId, @Nonnull BlockAction action, @Nonnull String blockId, boolean placedByPlayer) {
        this.playerId = playerId;
        this.action = action;
        this.blockId = blockId;
        this.placedByPlayer = placedByPlayer;
    }

    @Override
    public void progress(BlockActionQuestProgression<?> quest) {
        if (!quest.getPlayers().contains(playerId)) return;
        if (quest.isOver()) return;

        if (!quest.getBlockFilter().matches(blockId)) return;
        if (!quest.act(playerId, action, placedByPlayer)) return;

        quest.setState(quest.checkCompletion() ? QuestState.SUCCESSFUL : QuestState.IN_PROGRESS)
             .markDirty();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<BlockActionQuestProgression<?>> getQuestType() {
        return (Class<BlockActionQuestProgression<?>>) (Class<?>) BlockActionQuestProgression.class;
    }

    @Nonnull
    @Override
    public UUID getActorId() {
        return playerId;
    }
}
