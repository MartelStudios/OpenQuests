package com.martelstudios.openquests.extension.quests.placeblock;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.extension.quests.block.BlockActionQuestAsset;
import com.martelstudios.openquests.extension.quests.item.QuestItemFilter;

/**
 * Place a number of blocks.
 */
public class PlaceBlockQuestAsset extends BlockActionQuestAsset {

    public static final BuilderCodec<PlaceBlockQuestAsset> CODEC =
        BuilderCodec.builder(PlaceBlockQuestAsset.class, PlaceBlockQuestAsset::new, BlockActionQuestAsset.BASE_CODEC)
            .append(new KeyedCodec<>("BlockToPlace", QuestItemFilter.CODEC), (asset, block) -> asset.blockToPlace = block, asset -> asset.blockToPlace)
            .add()
            .build();

    protected QuestItemFilter blockToPlace;

    private PlaceBlockQuestAsset() {}

    @Override
    public AbstractQuestProgression<?> create() {
        return new PlaceBlockQuestProgression().setAssetId(getId());
    }

    /**
     * @return the blocks counted, unless a running quest names its own.
     */
    public QuestItemFilter getBlockToPlace() {
        return blockToPlace;
    }
}
