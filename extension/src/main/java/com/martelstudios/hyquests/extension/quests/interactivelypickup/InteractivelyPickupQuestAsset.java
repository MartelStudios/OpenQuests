package com.martelstudios.hyquests.extension.quests.interactivelypickup;

import com.hypixel.hytale.builtin.adventure.objectives.config.task.BlockTagOrItemIdField;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.hyquests.core.models.AbstractQuestProgression;
import com.martelstudios.hyquests.extension.quests.quantity.QuantityQuestAsset;

public class InteractivelyPickupQuestAsset extends QuantityQuestAsset {

    public static final BuilderCodec<InteractivelyPickupQuestAsset> CODEC =
        BuilderCodec.builder(InteractivelyPickupQuestAsset.class, InteractivelyPickupQuestAsset::new, QuantityQuestAsset.BASE_CODEC)
            .append(new KeyedCodec<>("ItemToPickup", BlockTagOrItemIdField.CODEC), (asset, item) -> asset.itemToPickup = item, asset -> asset.itemToPickup)
            .add()
            .build();

    protected BlockTagOrItemIdField itemToPickup;

    private InteractivelyPickupQuestAsset() {}

    @Override
    public AbstractQuestProgression<?> create() {
        return new InteractivelyPickupQuestProgression().setAssetId(getId());
    }

    public BlockTagOrItemIdField getItemToPickup() {
        return itemToPickup;
    }
}
