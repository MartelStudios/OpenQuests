package com.martelstudios.openquests.extension.quests.movement;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestAsset;

/**
 * Sprint a number of metres. {@code TargetQuantity} is the distance.
 */
public class SprintQuestAsset extends QuantityQuestAsset {

    public static final BuilderCodec<SprintQuestAsset> CODEC =
        BuilderCodec.builder(SprintQuestAsset.class, SprintQuestAsset::new, QuantityQuestAsset.BASE_CODEC)
                    .build();

    private SprintQuestAsset() {}

    @Override
    public AbstractQuestProgression<?> create() {
        return new SprintQuestProgression().setAssetId(getId());
    }
}
