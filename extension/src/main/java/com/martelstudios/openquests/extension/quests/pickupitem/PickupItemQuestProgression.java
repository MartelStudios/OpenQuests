package com.martelstudios.openquests.extension.quests.pickupitem;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.Message;
import com.martelstudios.openquests.extension.quests.item.ItemExchange;
import com.martelstudios.openquests.extension.quests.item.ItemExchangeQuestProgression;
import com.martelstudios.openquests.extension.quests.item.QuestItemFilter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class PickupItemQuestProgression extends ItemExchangeQuestProgression<PickupItemQuestProgression> {

    public static final BuilderCodec<PickupItemQuestProgression> CODEC = BuilderCodec.builder(PickupItemQuestProgression.class, PickupItemQuestProgression::new, ItemExchangeQuestProgression.BASE_CODEC)
                                                                              .append(new KeyedCodec<>("ItemToPickup", QuestItemFilter.CODEC), (quest, item) -> quest.itemToPickup = item, quest -> quest.itemToPickup)
                                                                              .add()
                                                                              .build();

    @Nullable
    protected QuestItemFilter itemToPickup;

    @Override
    public PickupItemQuestAsset getAsset() {
        return (PickupItemQuestAsset) super.getAsset();
    }

    /**
     * @return this instance's item if one was set on it, the asset's otherwise.
     */
    @Nonnull
    public QuestItemFilter getItemToPickup() {
        return itemToPickup != null ? itemToPickup : getAsset().getItemToPickup();
    }

    /**
     * Overrides the asset's item for this instance only.
     */
    public PickupItemQuestProgression setItemToPickup(@Nullable QuestItemFilter itemToPickup) {
        this.itemToPickup = itemToPickup;
        return this;
    }

    @Nonnull
    @Override
    public QuestItemFilter getItemFilter() {
        return getItemToPickup();
    }

    @Override
    protected boolean counts(@Nonnull ItemExchange exchange) {
        return exchange != ItemExchange.THROW;
    }

    @Nonnull
    @Override
    public Message getDefaultTitle() {
        var asset = getAsset();
        if (asset == null || asset.getItemToPickup() == null) return super.getDefaultTitle();

        return countedTitle("openquests.quest.default.pickup", asset.getItemToPickup());
    }
}
