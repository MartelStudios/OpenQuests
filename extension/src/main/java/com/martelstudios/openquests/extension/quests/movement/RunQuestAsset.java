package com.martelstudios.openquests.extension.quests.movement;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestAsset;

/**
 * Run a number of metres. {@code TargetQuantity} is the distance.
 */
public class RunQuestAsset extends QuantityQuestAsset {

    public static final BuilderCodec<RunQuestAsset> CODEC =
        BuilderCodec.builder(RunQuestAsset.class, RunQuestAsset::new, QuantityQuestAsset.BASE_CODEC)
                    .build();

    private RunQuestAsset() {}

    @Override
    public AbstractQuestProgression<?> create() {
        return new RunQuestProgression().setAssetId(getId());
    }
}
