package com.martelstudios.hyquests.visitors;

import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.console.ConsoleSender;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.hyquests.models.GatherQuest;
import com.martelstudios.hyquests.models.QuestState;

import java.util.UUID;

/**
 * Progresses {@link GatherQuest}s on inventory change, mirroring Hytale's {@code GatherObjectiveTask}:
 * rather than accumulating a specific pickup event, it recounts how many of the target item the
 * player currently holds and compares that to the asset's target quantity.
 */
public class GatherQuestVisitor implements QuestVisitor<GatherQuest> {
    private final UUID playerId;
    private final CombinedItemContainer combinedItemContainer;

    public GatherQuestVisitor(UUID playerId, Ref<EntityStore> playerRef, Store<EntityStore> store) {
        this.playerId = playerId;
        this.combinedItemContainer = store.getComponent(playerRef, InventoryComponent.Combined.getComponentType())
                                          .getInventories()
                                          .get(InventoryComponent.EVERYTHING);
    }

    public GatherQuestVisitor(UUID playerId, Holder<EntityStore> holder) {
        this.playerId = playerId;

        ItemContainer[] containers = new ItemContainer[InventoryComponent.EVERYTHING.length];

        var everything = InventoryComponent.EVERYTHING;
        for (int i = 0; i < everything.length; i++) {
            var inventory = everything[i];
            containers[i] = holder.getComponent(inventory).getInventory();
        }

        this.combinedItemContainer = new CombinedItemContainer(containers);
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

        ConsoleSender.INSTANCE.sendMessage(Message.raw("Quest progress: " + quest.getCount() + "/" + quest.getAsset()
                                                                                                          .getCount()));
    }

    @Override
    public Class<GatherQuest> getQuestType() {
        return GatherQuest.class;
    }
}
