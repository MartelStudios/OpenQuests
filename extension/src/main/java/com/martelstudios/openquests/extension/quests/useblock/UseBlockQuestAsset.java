package com.martelstudios.openquests.extension.quests.useblock;

import com.hypixel.hytale.builtin.adventure.objectives.config.task.BlockTagOrItemIdField;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestAsset;

/**
 * Interact with a block a number of times.
 */
public class UseBlockQuestAsset extends QuantityQuestAsset {

    public static final BuilderCodec<UseBlockQuestAsset> CODEC =
        BuilderCodec.builder(UseBlockQuestAsset.class, UseBlockQuestAsset::new, QuantityQuestAsset.BASE_CODEC)
                    .append(new KeyedCodec<>("BlockToUse", BlockTagOrItemIdField.CODEC), (asset, block) -> asset.blockToUse = block, asset -> asset.blockToUse)
                    .add()
                    .build();

    protected BlockTagOrItemIdField blockToUse;

    private UseBlockQuestAsset() {}

    @Override
    public AbstractQuestProgression<?> create() {
        return new UseBlockQuestProgression().setAssetId(getId());
    }

    public BlockTagOrItemIdField getBlockToUse() {
        return blockToUse;
    }
}
