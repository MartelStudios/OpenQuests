package com.martelstudios.openquests.extension.quests.movement;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestAsset;

/**
 * Jump a number of times. {@code TargetQuantity} is the count.
 */
public class JumpQuestAsset extends QuantityQuestAsset {

    public static final BuilderCodec<JumpQuestAsset> CODEC =
        BuilderCodec.builder(JumpQuestAsset.class, JumpQuestAsset::new, QuantityQuestAsset.BASE_CODEC)
                    .build();

    private JumpQuestAsset() {}

    @Override
    public AbstractQuestProgression<?> create() {
        return new JumpQuestProgression().setAssetId(getId());
    }
}
