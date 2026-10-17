package com.martelstudios.openquests.extension.quests.pickupitem;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.Message;
import com.martelstudios.openquests.extension.quests.item.ItemExchange;
import com.martelstudios.openquests.extension.quests.item.ItemExchangeQuestProgression;
import com.martelstudios.openquests.extension.quests.item.QuestItemFilter;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestProgression;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class PickupItemQuestProgression extends ItemExchangeQuestProgression<PickupItemQuestProgression> {

    public static final BuilderCodec<PickupItemQuestProgression> CODEC = BuilderCodec.builder(PickupItemQuestProgression.class, PickupItemQuestProgression::new, ItemExchangeQuestProgression.BASE_CODEC)
                                                                              .append(new KeyedCodec<>("ItemToPickup", QuestItemFilter.CODEC), (quest, item) -> quest.itemToPickup = item, quest -> quest.itemToPickup)
                                                                              .add()
                                                                              .build();

    @Nullable
    private QuestItemFilter itemToPickup;

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

    protected PickupItemQuestProgression() {}

    /**
     * Sets up a pickup quest, whose item can be overridden for that quest alone.
     */
    public static final class Builder extends QuantityQuestProgression.Builder<PickupItemQuestProgression, Builder> {
        Builder(@Nonnull PickupItemQuestAsset asset) {
            super(new PickupItemQuestProgression(), asset.getId());
        }

        /**
         * Overrides the asset's item for this quest alone.
         */
        public Builder itemToPickup(@Nullable QuestItemFilter itemToPickup) {
            quest().itemToPickup = itemToPickup;
            return this;
        }
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
        if (asset == null || getItemToPickup() == null) return super.getDefaultTitle();

        return countedTitle("openquests.quest.default.pickup", getItemToPickup());
    }
}
