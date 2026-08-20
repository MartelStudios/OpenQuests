package com.martelstudios.hyquests.extension.quests.reachlocation;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.hyquests.core.models.AbstractQuestProgression;
import com.martelstudios.hyquests.extension.quests.count.CountQuestProgression;

/**
 * Quest completed by a player simply being within the asset's radius of its target position.
 * No progress counter (unlike {@link CountQuestProgression}) — either the player is within range or not.
 */
public class ReachLocationQuestProgression extends AbstractQuestProgression<ReachLocationQuestProgression> {

    public static final BuilderCodec<ReachLocationQuestProgression> CODEC = BuilderCodec.builder(ReachLocationQuestProgression.class, ReachLocationQuestProgression::new, AbstractQuestProgression.BASE_CODEC)
                                                                                        .build();

    @Override
    public ReachLocationQuestAsset getAsset() {
        return (ReachLocationQuestAsset) super.getAsset();
    }
}
