package com.martelstudios.hyquests.extension.gather;

import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.hyquests.core.models.QuestState;
import com.martelstudios.hyquests.core.visitors.QuestVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import javax.annotation.Nonnull;

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
    public GatherQuestVisitor(Ref<EntityStore> playerRef) {
        var store = playerRef.getStore();
        this.playerId = store.getComponent(playerRef, UUIDComponent.getComponentType()).getUuid();

        List<ItemContainer> containers = new ArrayList<>(InventoryComponent.EVERYTHING.length);
        for (var inventoryType : InventoryComponent.EVERYTHING) {
            var inventory = store.getComponent(playerRef, inventoryType);
            if (inventory != null) containers.add(inventory.getInventory());
        }

        this.combinedItemContainer = new CombinedItemContainer(containers.toArray(new ItemContainer[0]));
    }

    public GatherQuestVisitor(Holder<EntityStore> playerHolder) {
        this.playerId = playerHolder.getComponent(UUIDComponent.getComponentType()).getUuid();

        List<ItemContainer> containers = new ArrayList<>(InventoryComponent.EVERYTHING.length);
        for (var inventoryType : InventoryComponent.EVERYTHING) {
            var inventory = playerHolder.getComponent(inventoryType);
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

        LOGGER.at(Level.FINE)
              .log("Quest %s progress for %s: %d/%d", quest.getId(), playerId, quest.getCount(), asset.getCount());
    }

    @Override
    public Class<GatherQuest> getQuestType() {
        return GatherQuest.class;
    }
}
