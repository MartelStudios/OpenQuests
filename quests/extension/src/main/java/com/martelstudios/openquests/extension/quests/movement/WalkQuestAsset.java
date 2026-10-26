package com.martelstudios.openquests.extension.quests.movement;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestProgression;

/**
 * Walk a number of metres, or for a number of seconds. {@code TargetQuantity} is the distance or the
 * duration, as {@code Measure} says.
 */
public class WalkQuestAsset extends TravelQuestAsset {

    public static final BuilderCodec<WalkQuestAsset> CODEC =
        BuilderCodec.builder(WalkQuestAsset.class, WalkQuestAsset::new, TravelQuestAsset.BASE_CODEC)
                    .build();

    private WalkQuestAsset() {}

    @Override
    public QuantityQuestProgression.Builder<WalkQuestProgression, ?> builder() {
        return QuantityQuestProgression.Builder.of(new WalkQuestProgression(), getId());
    }
}
