package com.martelstudios.hyquests.models;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.hyquests.assets.ReachLocationQuestAsset;
import com.martelstudios.hyquests.visitors.QuestVisitor;

/**
 * Quest completed by a player simply being within the asset's radius of its target position.
 * No progress counter (unlike {@link CountQuest}) — either the player is within range or not.
 */
public class ReachLocationQuest extends AbstractQuest<ReachLocationQuest> {

    public static final BuilderCodec<ReachLocationQuest> CODEC = BuilderCodec.builder(ReachLocationQuest.class, ReachLocationQuest::new, AbstractQuest.BASE_CODEC)
                                                                              .build();

    @Override
    public ReachLocationQuestAsset getAsset() {
        return (ReachLocationQuestAsset) super.getAsset();
    }
}
