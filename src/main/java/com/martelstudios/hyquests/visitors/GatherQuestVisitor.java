package com.martelstudios.hyquests.visitors;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.martelstudios.hyquests.PlayerAccess;
import com.martelstudios.hyquests.models.GatherQuest;
import com.martelstudios.hyquests.models.QuestState;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Progresses {@link GatherQuest}s on inventory change, mirroring Hytale's {@code GatherObjectiveTask}:
 * rather than accumulating a specific pickup event, it recounts how many of the target item the
 * player currently holds and compares that to the asset's target quantity.
 */
public class GatherQuestVisitor implements QuestVisitor<GatherQuest> {
    @Nonnull
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final UUID playerId;
    private final CombinedItemContainer combinedItemContainer;

    /**
     * Reads the inventories one by one rather than through {@code InventoryComponent.Combined},
     * which only exists on a live entity: this way an online and an offline player are counted
     * exactly the same way.
     */
    public GatherQuestVisitor(@Nonnull UUID playerId, @Nonnull PlayerAccess access) {
        this.playerId = playerId;

        List<ItemContainer> containers = new ArrayList<>(InventoryComponent.EVERYTHING.length);
        for (var inventoryType : InventoryComponent.EVERYTHING) {
            var inventory = access.getComponent(inventoryType);
            if (inventory != null) containers.add(inventory.getInventory());
        }

        this.combinedItemContainer = new CombinedItemContainer(containers.toArray(new ItemContainer[0]));
    }

    @Override
    public void progress(GatherQuest quest) {
        if (!quest.getPlayers().contains(playerId)) return;
        if (quest.getState() == QuestState.SUCCESSFUL) return;

        var asset = quest.getAsset();
        int count = combinedItemContainer.countItemStacks(itemStack -> itemStack.getItemId()
                                                                                .equals(asset.getItemToGather()));

        quest.setCount(count)
             .setState(quest.checkCompletion() ? QuestState.SUCCESSFUL : QuestState.IN_PROGRESS)
             .markDirty();

        LOGGER.at(Level.FINE).log("Quest %s progress for %s: %d/%d", quest.getId(), playerId, quest.getCount(), asset.getCount());
    }

    @Override
    public Class<GatherQuest> getQuestType() {
        return GatherQuest.class;
    }
}
