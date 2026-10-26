package com.martelstudios.openquests.extension.quests.interactivelypickup;

import com.hypixel.hytale.server.core.Message;

import javax.annotation.Nonnull;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.extension.quests.item.QuestItemFilter;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestProgression;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class InteractivelyPickupQuestProgression extends QuantityQuestProgression<InteractivelyPickupQuestProgression> {

    public static final BuilderCodec<InteractivelyPickupQuestProgression> CODEC = BuilderCodec.builder(InteractivelyPickupQuestProgression.class, InteractivelyPickupQuestProgression::new, QuantityQuestProgression.BASE_CODEC)
                                                                                              .append(new KeyedCodec<>("ItemToPickup", QuestItemFilter.CODEC), (quest, item) -> quest.itemToPickup = item, quest -> quest.itemToPickup)
                                                                                              .add()
                                                                                              .build();

    @Nullable
    private QuestItemFilter itemToPickup;

    @Override
    public InteractivelyPickupQuestAsset getAsset() {
        return (InteractivelyPickupQuestAsset) super.getAsset();
    }

    /**
     * @return this instance's item if one was set on it, the asset's otherwise.
     */
    @Nonnull
    public QuestItemFilter getItemToPickup() {
        return itemToPickup != null ? itemToPickup : getAsset().getItemToPickup();
    }

    protected InteractivelyPickupQuestProgression() {}

    /**
     * Sets up an interactive pickup quest, whose item can be overridden for that quest alone.
     */
    public static final class Builder extends QuantityQuestProgression.Builder<InteractivelyPickupQuestProgression, Builder> {
        Builder(@Nonnull InteractivelyPickupQuestAsset asset) {
            super(new InteractivelyPickupQuestProgression(), asset.getId());
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
    public Message getDefaultTitle() {
        var asset = getAsset();
        if (asset == null || getItemToPickup() == null) return super.getDefaultTitle();

        return countedTitle("openquests.quest.default.pickup", getItemToPickup());
    }
}
