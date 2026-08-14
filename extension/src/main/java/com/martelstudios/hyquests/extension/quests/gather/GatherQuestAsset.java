package com.martelstudios.hyquests.extension.quests.gather;

import com.hypixel.hytale.builtin.adventure.objectives.config.task.BlockTagOrItemIdField;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.hyquests.core.models.AbstractQuest;
import com.martelstudios.hyquests.extension.quests.count.CountQuestAsset;

public class GatherQuestAsset extends CountQuestAsset {

    public static final BuilderCodec<GatherQuestAsset> CODEC =
        BuilderCodec.builder(GatherQuestAsset.class, GatherQuestAsset::new, CountQuestAsset.BASE_CODEC)
            .append(new KeyedCodec<>("ItemToGather", BlockTagOrItemIdField.CODEC), (asset, item) -> asset.itemToGather = item, asset -> asset.itemToGather)
            .add()
            .build();

    protected BlockTagOrItemIdField itemToGather;

    private GatherQuestAsset() {}

    @Override
    public AbstractQuest<?> create() {
        return new GatherQuest().setQuestAssetId(getId());
    }

    public BlockTagOrItemIdField getItemToGather() {
        return itemToGather;
    }
}
