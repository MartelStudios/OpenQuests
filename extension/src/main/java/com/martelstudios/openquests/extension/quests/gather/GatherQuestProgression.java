package com.martelstudios.openquests.extension.quests.gather;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.extension.quests.item.QuestItemFilter;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestProgression;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Quest whose progress is however many of a given item the player currently holds, rechecked
 * on every inventory change.
 */
public class GatherQuestProgression extends QuantityQuestProgression<GatherQuestProgression> {

    public static final BuilderCodec<GatherQuestProgression> CODEC = BuilderCodec.builder(GatherQuestProgression.class, GatherQuestProgression::new, QuantityQuestProgression.BASE_CODEC)
                                                                                 .append(new KeyedCodec<>("ItemToGather", QuestItemFilter.CODEC), (quest, item) -> quest.itemToGather = item, quest -> quest.itemToGather)
                                                                                 .add()
                                                                                 .build();

    @Nullable
    private QuestItemFilter itemToGather;

    @Override
    public GatherQuestAsset getAsset() {
        return (GatherQuestAsset) super.getAsset();
    }

    /**
     * @return this instance's item if one was set on it, the asset's otherwise.
     */
    @Nonnull
    public QuestItemFilter getItemToGather() {
        return itemToGather != null ? itemToGather : getAsset().getItemToGather();
    }

    protected GatherQuestProgression() {}

    /**
     * Sets up a gather quest, whose item can be overridden for that quest alone.
     */
    public static final class Builder extends QuantityQuestProgression.Builder<GatherQuestProgression, Builder> {
        Builder(@Nonnull GatherQuestAsset asset) {
            super(new GatherQuestProgression(), asset.getId());
        }

        /**
         * Overrides the asset's item for this quest alone.
         */
        public Builder itemToGather(@Nullable QuestItemFilter itemToGather) {
            quest().itemToGather = itemToGather;
            return this;
        }
    }

    @Nonnull
    @Override
    public Message getDefaultTitle() {
        var asset = getAsset();
        if (asset == null || asset.getItemToGather() == null) return super.getDefaultTitle();

        return countedTitle("openquests.quest.default.gather", asset.getItemToGather());
    }
}
