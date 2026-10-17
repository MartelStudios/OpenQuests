package com.martelstudios.openquests.extension.quests.placeblock;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.extension.quests.block.BlockAction;
import com.martelstudios.openquests.extension.quests.block.BlockActionQuestProgression;
import com.martelstudios.openquests.extension.quests.item.QuestItemFilter;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestProgression;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class PlaceBlockQuestProgression extends BlockActionQuestProgression<PlaceBlockQuestProgression> {

    public static final BuilderCodec<PlaceBlockQuestProgression> CODEC = BuilderCodec.builder(PlaceBlockQuestProgression.class, PlaceBlockQuestProgression::new, BlockActionQuestProgression.BASE_CODEC)
                                                                              .append(new KeyedCodec<>("BlockToPlace", QuestItemFilter.CODEC), (quest, block) -> quest.blockToPlace = block, quest -> quest.blockToPlace)
                                                                              .add()
                                                                              .build();

    /**
     * Overrides the asset's block for this instance alone.
     */
    @Nullable
    protected QuestItemFilter blockToPlace;

    @Override
    public PlaceBlockQuestAsset getAsset() {
        return (PlaceBlockQuestAsset) super.getAsset();
    }

    /**
     * @return this instance's block if one was set on it, the asset's otherwise.
     */
    @Nonnull
    public QuestItemFilter getBlockToPlace() {
        return blockToPlace != null ? blockToPlace : getAsset().getBlockToPlace();
    }

    protected PlaceBlockQuestProgression() {}

    /**
     * Sets up a place quest, whose block can be overridden for that quest alone.
     */
    public static final class Builder extends QuantityQuestProgression.Builder<PlaceBlockQuestProgression, Builder> {
        Builder(@Nonnull PlaceBlockQuestAsset asset) {
            super(new PlaceBlockQuestProgression(), asset.getId());
        }

        /**
         * Overrides the asset's block for this quest alone.
         */
        public Builder blockToPlace(@Nullable QuestItemFilter blockToPlace) {
            quest().blockToPlace = blockToPlace;
            return this;
        }
    }

    @Nonnull
    @Override
    public QuestItemFilter getBlockFilter() {
        return getBlockToPlace();
    }

    @Override
    protected boolean counts(@Nonnull BlockAction action) {
        return action == BlockAction.PLACE;
    }
}
