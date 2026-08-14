package com.martelstudios.hyquests.extension.quests.interactivelypickup;

import com.hypixel.hytale.builtin.adventure.objectives.config.task.BlockTagOrItemIdField;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.hyquests.core.models.AbstractQuest;
import com.martelstudios.hyquests.extension.quests.count.CountQuestAsset;

public class InteractivelyPickupQuestAsset extends CountQuestAsset {

    public static final BuilderCodec<InteractivelyPickupQuestAsset> CODEC =
        BuilderCodec.builder(InteractivelyPickupQuestAsset.class, InteractivelyPickupQuestAsset::new, CountQuestAsset.BASE_CODEC)
            .append(new KeyedCodec<>("ItemToPickup", BlockTagOrItemIdField.CODEC), (asset, item) -> asset.itemToPickup = item, asset -> asset.itemToPickup)
            .add()
            .build();

    protected BlockTagOrItemIdField itemToPickup;

    private InteractivelyPickupQuestAsset() {}

    @Override
    public AbstractQuest<?> create() {
        return new InteractivelyPickupQuest().setQuestAssetId(getId());
    }

    public BlockTagOrItemIdField getItemToPickup() {
        return itemToPickup;
    }
}
