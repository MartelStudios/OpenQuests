package com.martelstudios.openquests.extension.quests.wait;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestAsset;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestProgression;

/**
 * Spend a number of seconds in game, whatever the player does. {@code TargetQuantity} is the
 * duration; where and how it counts is left to the constraints, such as {@code InWorld}.
 */
public class WaitQuestAsset extends QuantityQuestAsset {

    public static final BuilderCodec<WaitQuestAsset> CODEC =
        BuilderCodec.builder(WaitQuestAsset.class, WaitQuestAsset::new, QuantityQuestAsset.BASE_CODEC)
                    .build();

    private WaitQuestAsset() {}

    @Override
    public QuantityQuestProgression.Builder<WaitQuestProgression, ?> builder() {
        return QuantityQuestProgression.Builder.of(new WaitQuestProgression(), getId());
    }
}
