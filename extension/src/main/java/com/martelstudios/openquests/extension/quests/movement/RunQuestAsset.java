package com.martelstudios.openquests.extension.quests.movement;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;

/**
 * Run a number of metres, or for a number of seconds. {@code TargetQuantity} is the distance or the
 * duration, as {@code Measure} says.
 */
public class RunQuestAsset extends TravelQuestAsset {

    public static final BuilderCodec<RunQuestAsset> CODEC =
        BuilderCodec.builder(RunQuestAsset.class, RunQuestAsset::new, TravelQuestAsset.BASE_CODEC)
                    .build();

    private RunQuestAsset() {}

    @Override
    public AbstractQuestProgression<?> create() {
        return new RunQuestProgression().setAssetId(getId());
    }
}
