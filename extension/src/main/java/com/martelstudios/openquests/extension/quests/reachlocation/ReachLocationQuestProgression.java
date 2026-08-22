package com.martelstudios.openquests.extension.quests.reachlocation;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;

/**
 * Quest completed by a player simply being within the asset's radius of its target position.
 */
public class ReachLocationQuestProgression extends AbstractQuestProgression<ReachLocationQuestProgression> {

    public static final BuilderCodec<ReachLocationQuestProgression> CODEC = BuilderCodec.builder(ReachLocationQuestProgression.class, ReachLocationQuestProgression::new, AbstractQuestProgression.BASE_CODEC)
                                                                                        .build();

    @Override
    public ReachLocationQuestAsset getAsset() {
        return (ReachLocationQuestAsset) super.getAsset();
    }
}
