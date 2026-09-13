package com.martelstudios.openquests.extension.quests.placeblock;

import com.hypixel.hytale.builtin.adventure.objectives.config.task.BlockTagOrItemIdField;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestAsset;

/**
 * Place a number of blocks.
 */
public class PlaceBlockQuestAsset extends QuantityQuestAsset {

    public static final BuilderCodec<PlaceBlockQuestAsset> CODEC =
        BuilderCodec.builder(PlaceBlockQuestAsset.class, PlaceBlockQuestAsset::new, QuantityQuestAsset.BASE_CODEC)
                    .append(new KeyedCodec<>("BlockToPlace", BlockTagOrItemIdField.CODEC), (asset, block) -> asset.blockToPlace = block, asset -> asset.blockToPlace)
                    .add()
                    .build();

    protected BlockTagOrItemIdField blockToPlace;

    private PlaceBlockQuestAsset() {}

    @Override
    public AbstractQuestProgression<?> create() {
        return new PlaceBlockQuestProgression().setAssetId(getId());
    }

    public BlockTagOrItemIdField getBlockToPlace() {
        return blockToPlace;
    }
}
