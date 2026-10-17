package com.martelstudios.openquests.extension.quests.consumeitem;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.Message;
import com.martelstudios.openquests.extension.quests.item.QuestItemFilter;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestProgression;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ConsumeItemQuestProgression extends QuantityQuestProgression<ConsumeItemQuestProgression> {

    public static final BuilderCodec<ConsumeItemQuestProgression> CODEC = BuilderCodec.builder(ConsumeItemQuestProgression.class, ConsumeItemQuestProgression::new, QuantityQuestProgression.BASE_CODEC)
                                                                                      .append(new KeyedCodec<>("ItemToConsume", QuestItemFilter.CODEC), (quest, item) -> quest.itemToConsume = item, quest -> quest.itemToConsume)
                                                                                      .add()
                                                                                      .build();

    /**
     * Overrides the asset's item for this instance alone.
     */
    @Nullable
    protected QuestItemFilter itemToConsume;

    @Override
    public ConsumeItemQuestAsset getAsset() {
        return (ConsumeItemQuestAsset) super.getAsset();
    }

    /**
     * @return this instance's item if one was set on it, the asset's otherwise.
     */
    @Nonnull
    public QuestItemFilter getItemToConsume() {
        return itemToConsume != null ? itemToConsume : getAsset().getItemToConsume();
    }

    protected ConsumeItemQuestProgression() {}

    /**
     * Sets up a consume quest, whose item can be overridden for that quest alone.
     */
    public static final class Builder extends QuantityQuestProgression.Builder<ConsumeItemQuestProgression, Builder> {
        Builder(@Nonnull ConsumeItemQuestAsset asset) {
            super(new ConsumeItemQuestProgression(), asset.getId());
        }

        /**
         * Overrides the asset's item for this quest alone.
         */
        public Builder itemToConsume(@Nullable QuestItemFilter itemToConsume) {
            quest().itemToConsume = itemToConsume;
            return this;
        }
    }

    @Nonnull
    @Override
    public Message getDefaultTitle() {
        var asset = getAsset();
        if (asset == null || asset.getItemToConsume() == null) return super.getDefaultTitle();

        return countedTitle("openquests.quest.default.consume", asset.getItemToConsume());
    }
}
