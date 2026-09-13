package com.martelstudios.openquests.extension.quests.consumeitem;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.event.events.ecs.InventoryChangeEvent;
import com.hypixel.hytale.server.core.inventory.ActiveSlotInventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.transaction.ActionType;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackSlotTransaction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.core.stores.QuestStoreComponent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Reads eating and drinking off the inventory, since the server fires no event of its own for it:
 * a consumable's interaction chain ends on {@code ModifyInventory} with a negative
 * {@code AdjustHeldItemQuantity}, which takes the item out of the slot the player is holding.
 */
public class ConsumeItemEventSystem extends EntityEventSystem<EntityStore, InventoryChangeEvent> {
    public ConsumeItemEventSystem() {
        super(InventoryChangeEvent.class);
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull InventoryChangeEvent event) {
        // Only a held item can be consumed, so only a section with an active slot is worth reading
        if (!(event.getInventory() instanceof ActiveSlotInventoryComponent inventory)) return;

        byte activeSlot = inventory.getActiveSlot();
        if (activeSlot < 0) return;

        if (!(event.getTransaction() instanceof ItemStackSlotTransaction removal)) return;
        if (!removal.succeeded() || removal.getAction() != ActionType.REMOVE) return;
        if (removal.getSlot() != activeSlot) return;

        // Saying which item to take, rather than letting the slot be read for it, is what an
        // interaction spending the held item does. Dropping names a quantity and no item, so it
        // leaves a null query; moving leaves a MoveTransaction. Placing a block is the one other
        // thing that reaches here, and the item not being consumable rules it out.
        if (removal.getQuery() == null) return;

        ItemStack consumed = removal.getOutput();
        if (consumed == null || consumed.isEmpty()) return;
        if (!consumed.getItem().isConsumable()) return;

        var ref = chunk.getReferenceTo(index);

        var playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        var questStoreComponent = store.getComponent(ref, QuestStoreComponent.getComponentType());
        if (playerRef == null || questStoreComponent == null) return;

        QuestProgressionService.get()
                               .progress(new ConsumeItemQuestVisitor(playerRef.getUuid(), consumed.getItemId(), consumed.getQuantity()), questStoreComponent.getQuestIds());
    }

    @Override
    public @Nullable Query<EntityStore> getQuery() {
        return Query.and(PlayerRef.getComponentType(), QuestStoreComponent.getComponentType());
    }
}
