package com.martelstudios.openquests.extension.quests.placeblock;

import com.hypixel.hytale.builtin.adventure.objectives.config.task.BlockTagOrItemIdField;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestProgression;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class PlaceBlockQuestProgression extends QuantityQuestProgression<PlaceBlockQuestProgression> {

    public static final BuilderCodec<PlaceBlockQuestProgression> CODEC = BuilderCodec.builder(PlaceBlockQuestProgression.class, PlaceBlockQuestProgression::new, QuantityQuestProgression.BASE_CODEC)
                                                                                   .append(new KeyedCodec<>("BlockToPlace", BlockTagOrItemIdField.CODEC), (quest, block) -> quest.blockToPlace = block, quest -> quest.blockToPlace)
                                                                                   .add()
                                                                                   .build();

    /**
     * Overrides the asset's block for this instance alone.
     */
    @Nullable
    protected BlockTagOrItemIdField blockToPlace;

    @Override
    public PlaceBlockQuestAsset getAsset() {
        return (PlaceBlockQuestAsset) super.getAsset();
    }

    /**
     * @return this instance's block if one was set on it, the asset's otherwise.
     */
    @Nonnull
    public BlockTagOrItemIdField getBlockToPlace() {
        return blockToPlace != null ? blockToPlace : getAsset().getBlockToPlace();
    }

    public PlaceBlockQuestProgression setBlockToPlace(@Nullable BlockTagOrItemIdField blockToPlace) {
        this.blockToPlace = blockToPlace;
        return this;
    }
}
