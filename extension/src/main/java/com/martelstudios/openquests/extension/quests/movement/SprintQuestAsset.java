package com.martelstudios.openquests.extension.quests.movement;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;

/**
 * Sprint a number of metres, or for a number of seconds. {@code TargetQuantity} is the distance or the
 * duration, as {@code Measure} says.
 */
public class SprintQuestAsset extends TravelQuestAsset {

    public static final BuilderCodec<SprintQuestAsset> CODEC =
        BuilderCodec.builder(SprintQuestAsset.class, SprintQuestAsset::new, TravelQuestAsset.BASE_CODEC)
                    .build();

    private SprintQuestAsset() {}

    @Override
    public AbstractQuestProgression<?> create() {
        return new SprintQuestProgression().setAssetId(getId());
    }
}
