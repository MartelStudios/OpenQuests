package com.martelstudios.openquests.extension.quests.placeblock;

import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.visitors.QuestVisitor;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Counts one placed block against the quests targeting it.
 *
 * <p>Matched on the item in hand rather than on a block type: placing is the one side of this pair
 * the server announces before the block exists, so the item about to become it is all there is to
 * go on. For a block that is placed from its own item — which is most of them — the two names are
 * the same, and a tag resolves the same way either way.
 */
public class PlaceBlockQuestVisitor implements QuestVisitor<PlaceBlockQuestProgression> {

    private final UUID playerId;
    private final PlaceBlockEvent event;

    public PlaceBlockQuestVisitor(@Nonnull UUID playerId, @Nonnull PlaceBlockEvent event) {
        this.playerId = playerId;
        this.event = event;
    }

    @Override
    public void progress(PlaceBlockQuestProgression quest) {
        if (!quest.getPlayers().contains(playerId)) return;
        if (quest.isCompleted() && quest.isStopOnComplete()) return;

        ItemStack itemInHand = event.getItemInHand();
        if (itemInHand == null || !quest.getBlockToPlace().isBlockTypeIncluded(itemInHand.getItemId())) return;

        quest.setCurrentQuantity(quest.getCurrentQuantity() + 1)
             .setState(quest.checkCompletion() ? QuestState.SUCCESSFUL : QuestState.IN_PROGRESS)
             .markDirty();
    }

    @Override
    public Class<PlaceBlockQuestProgression> getQuestType() {
        return PlaceBlockQuestProgression.class;
    }

    @Nonnull
    @Override
    public UUID getActorId() {
        return playerId;
    }
}
