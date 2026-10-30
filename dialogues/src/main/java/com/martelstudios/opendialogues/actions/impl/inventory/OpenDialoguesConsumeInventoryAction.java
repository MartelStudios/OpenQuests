package com.martelstudios.opendialogues.actions.impl.inventory;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.opendialogues.actions.DialogueAction;
import com.martelstudios.opendialogues.api.DialogueContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Takes a list of items from the player's inventory, all or nothing: when one entry is short,
 * nothing is taken. The next actions of the answer run either way.
 */
public class OpenDialoguesConsumeInventoryAction extends DialogueAction {

    public static final String TYPE = "ConsumeInventory";

    public static final BuilderCodec<OpenDialoguesConsumeInventoryAction> CODEC = BuilderCodec.builder(OpenDialoguesConsumeInventoryAction.class, OpenDialoguesConsumeInventoryAction::new, DialogueAction.BASE_CODEC)
                                                                                         .append(new KeyedCodec<>("Items", new ArrayCodec<>(OpenDialoguesItemQuantity.CODEC, OpenDialoguesItemQuantity[]::new), true), (action, items) -> action.items = items, action -> action.items)
                                                                                         .addValidator(Validators.nonNull())
                                                                                         .addValidator(Validators.nonEmptyArray())
                                                                                         .add()
                                                                                         .build();

    private OpenDialoguesItemQuantity[] items;

    /**
     * Defers to the player's world thread when called elsewhere, since only it may touch the inventory.
     */
    @Override
    public void execute(@Nonnull DialogueContext context) {
        Ref<EntityStore> entity = context.player().getReference();
        if (entity == null || !entity.isValid()) return;

        Store<EntityStore> store = entity.getStore();
        if (store.isInThread()) {
            consume(store, entity);
        } else {
            store.getExternalData().getWorld().execute(() -> {
                if (entity.isValid()) consume(store, entity);
            });
        }
    }

    private void consume(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> entity) {
        ItemContainer inventory = InventoryComponent.getCombined(store, entity, InventoryComponent.HOTBAR_FIRST);
        short capacity = inventory.getCapacity();
        String[] ids = new String[capacity];
        int[] quantities = new int[capacity];
        for (short slot = 0; slot < capacity; slot++) {
            ItemStack stack = inventory.getItemStack(slot);
            if (stack == null || stack.isEmpty()) continue;
            ids[slot] = stack.getItemId();
            quantities[slot] = stack.getQuantity();
        }

        int[] removals = removals(ids, quantities, List.of(items));
        if (removals == null) return;

        for (short slot = 0; slot < capacity; slot++) {
            if (removals[slot] > 0) inventory.removeItemStackFromSlot(slot, removals[slot]);
        }
    }

    /**
     * Takes from each entry in order, slot by slot, so two entries accepting the same item never
     * count one stack twice.
     *
     * @param slotIds the item in each slot, {@code null} when empty.
     * @return how many to take from each slot, or {@code null} when any entry is short.
     */
    @Nullable
    public static int[] removals(@Nonnull String[] slotIds, @Nonnull int[] slotQuantities, @Nonnull List<OpenDialoguesItemQuantity> items) {
        int[] left = slotQuantities.clone();
        int[] removals = new int[slotIds.length];
        for (OpenDialoguesItemQuantity entry : items) {
            int needed = entry.quantity();
            for (int slot = 0; slot < slotIds.length && needed > 0; slot++) {
                if (left[slot] <= 0 || !entry.item().matches(slotIds[slot])) continue;
                int taken = Math.min(left[slot], needed);
                left[slot] -= taken;
                removals[slot] += taken;
                needed -= taken;
            }
            if (needed > 0) return null;
        }
        return removals;
    }
}
