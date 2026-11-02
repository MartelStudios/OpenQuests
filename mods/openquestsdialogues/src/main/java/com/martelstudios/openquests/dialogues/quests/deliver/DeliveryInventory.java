package com.martelstudios.openquests.dialogues.quests.deliver;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.openquests.dialogues.quests.deliver.OpenQuestsDeliverAsset.DeliverItem;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * A player's inventory as slots of ids and quantities, read on their world thread.
 */
final class DeliveryInventory {

    final ItemContainer container;
    final String[] ids;
    final int[] quantities;

    private DeliveryInventory(@Nonnull ItemContainer container) {
        this.container = container;
        short capacity = container.getCapacity();
        ids = new String[capacity];
        quantities = new int[capacity];
        for (short slot = 0; slot < capacity; slot++) {
            ItemStack stack = container.getItemStack(slot);
            if (stack == null || stack.isEmpty()) continue;
            ids[slot] = stack.getItemId();
            quantities[slot] = stack.getQuantity();
        }
    }

    @Nonnull
    static DeliveryInventory of(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> player) {
        return new DeliveryInventory(InventoryComponent.getCombined(store, player, InventoryComponent.HOTBAR_FIRST));
    }

    /**
     * @return how many of each item the player carries, a stack matching two items counted for both.
     */
    @Nonnull
    int[] carried(@Nonnull DeliverItem[] items) {
        int[] carried = new int[items.length];
        for (int item = 0; item < items.length; item++) {
            for (int slot = 0; slot < ids.length; slot++) {
                if (ids[slot] != null && items[item].getItem().matches(ids[slot])) carried[item] += quantities[slot];
            }
        }
        return carried;
    }

    @Nonnull
    static List<Predicate<String>> filters(@Nonnull DeliverItem[] items) {
        List<Predicate<String>> filters = new ArrayList<>(items.length);
        for (DeliverItem item : items) filters.add(item.getItem()::matches);
        return filters;
    }
}
