package com.martelstudios.openquests.dialogues.quests.deliver;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.core.visitors.QuestVisitor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;

/**
 * Moves items from a player to their running delivery quests.
 */
public final class Deliveries {

    private Deliveries() {}

    /**
     * Takes from the player's inventory what each running quest of that asset they hold still
     * misses, then counts on the quest only what actually left the inventory. Runs on the player's
     * world thread.
     *
     * @param itemId the one item to hand over, {@code null} for any the quest accepts.
     * @return the last quest that received items, {@code null} for none.
     */
    @Nullable
    public static DeliverQuestProgression deliver(@Nonnull QuestProgressionService progression, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> player, @Nonnull UUID playerId, @Nonnull String assetId, @Nullable String itemId) {
        DeliverQuestProgression[] delivered = new DeliverQuestProgression[1];
        progression.progress(new QuestVisitor<DeliverQuestProgression>() {
            @Override
            public void progress(DeliverQuestProgression quest) {
                if (quest.isOver() || !quest.getPlayers().contains(playerId) || !assetId.equals(quest.getAssetId()) || quest.getAsset() == null) return;

                int[] missing = missing(quest);
                DeliveryInventory inventory = DeliveryInventory.of(store, player);
                String[] ids = itemId == null ? inventory.ids : DeliveryTake.only(inventory.ids, itemId);
                var filters = DeliveryInventory.filters(quest.getAsset().getItems());
                DeliveryTake.Taken planned = DeliveryTake.take(ids, inventory.quantities, filters, missing);

                int[] removed = new int[ids.length];
                for (short slot = 0; slot < ids.length; slot++) {
                    int wanted = planned.removals()[slot];
                    if (wanted <= 0) continue;
                    inventory.container.removeItemStackFromSlot(slot, wanted);
                    ItemStack left = inventory.container.getItemStack(slot);
                    int remaining = left == null || left.isEmpty() || !ids[slot].equals(left.getItemId()) ? 0 : left.getQuantity();
                    removed[slot] = Math.clamp(inventory.quantities[slot] - remaining, 0, wanted);
                }

                int[] credited = DeliveryTake.take(ids, removed, filters, missing).perItem();
                for (int i = 0; i < credited.length; i++) {
                    if (credited[i] > 0) quest.apply(new DeliverQuestProgression.Delivered(i, credited[i]));
                }
                delivered[0] = quest;
            }

            @Override
            public Class<DeliverQuestProgression> getQuestType() {
                return DeliverQuestProgression.class;
            }

            @Override
            public UUID getActorId() {
                return playerId;
            }
        }, progression.getQuestIdsForType(DeliverQuestProgression.class));
        return delivered[0];
    }

    /**
     * @return for each item id the player carries, how many its own button would hand to the quest.
     */
    @Nonnull
    static Map<String, Integer> givable(@Nonnull DeliverQuestProgression quest, @Nonnull DeliveryInventory inventory) {
        return DeliveryTake.givable(inventory.ids, inventory.quantities, DeliveryInventory.filters(quest.getAsset().getItems()), missing(quest));
    }

    @Nonnull
    static int[] missing(@Nonnull DeliverQuestProgression quest) {
        int[] missing = new int[quest.getAsset().getItems().length];
        for (int i = 0; i < missing.length; i++) missing[i] = quest.missing(i);
        return missing;
    }
}
