package com.martelstudios.openquests.extension.quests.movement;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestAsset;

/**
 * Walk a number of metres. {@code TargetQuantity} is the distance.
 */
public class WalkQuestAsset extends QuantityQuestAsset {

    public static final BuilderCodec<WalkQuestAsset> CODEC =
        BuilderCodec.builder(WalkQuestAsset.class, WalkQuestAsset::new, QuantityQuestAsset.BASE_CODEC)
                    .build();

    private WalkQuestAsset() {}

    @Override
    public AbstractQuestProgression<?> create() {
        return new WalkQuestProgression().setAssetId(getId());
    }
}
