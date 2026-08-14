package com.martelstudios.hyquests.extension.interactivelypickup;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.hyquests.core.models.AbstractQuest;
import com.martelstudios.hyquests.extension.count.CountQuestAsset;

public class InteractivelyPickupQuestAsset extends CountQuestAsset {

    public static final BuilderCodec<InteractivelyPickupQuestAsset> CODEC =
        BuilderCodec.builder(InteractivelyPickupQuestAsset.class, InteractivelyPickupQuestAsset::new, CountQuestAsset.BASE_CODEC)
            .append(new KeyedCodec<>("ItemToPickup", Codec.STRING), (asset, item) -> asset.itemToPickup = item, asset -> asset.itemToPickup)
            .add()
            .build();

    protected String itemToPickup;

    private InteractivelyPickupQuestAsset() {}

    @Override
    public AbstractQuest<?> create() {
        return new InteractivelyPickupQuest().setQuestAssetId(getId());
    }

    public String getItemToPickup() {
        return itemToPickup;
    }
}
