package com.martelstudios.openquests.dialogues.quests.deliver;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.core.visitors.QuestVisitor;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Moves items from a player to their running delivery quests.
 */
public final class Deliveries {

    private Deliveries() {}

    /**
     * Takes from the player's inventory what each running quest of that asset they hold still
     * misses, then counts it on the quest. Runs on the player's world thread.
     */
    public static void deliver(@Nonnull QuestProgressionService progression, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> player, @Nonnull UUID playerId, @Nonnull String assetId) {
        progression.progress(new QuestVisitor<DeliverQuestProgression>() {
            @Override
            public void progress(DeliverQuestProgression quest) {
                if (quest.isOver() || !quest.getPlayers().contains(playerId) || !assetId.equals(quest.getAssetId()) || quest.getAsset() == null) return;

                OpenQuestsDeliverAsset asset = quest.getAsset();
                int[] missing = new int[asset.getItems().length];
                for (int i = 0; i < missing.length; i++) missing[i] = quest.missing(i);

                DeliveryInventory inventory = DeliveryInventory.of(store, player);
                DeliveryTake.Taken taken = DeliveryTake.take(inventory.ids, inventory.quantities, DeliveryInventory.filters(asset.getItems()), missing);
                for (short slot = 0; slot < taken.removals().length; slot++) {
                    if (taken.removals()[slot] > 0) inventory.container.removeItemStackFromSlot(slot, taken.removals()[slot]);
                }
                for (int i = 0; i < taken.perItem().length; i++) {
                    if (taken.perItem()[i] > 0) quest.apply(new DeliverQuestProgression.Delivered(i, taken.perItem()[i]));
                }
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
    }
}
