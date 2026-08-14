package com.martelstudios.hyquests.extension.reachlocation;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.hyquests.core.models.AbstractQuest;
import com.martelstudios.hyquests.core.visitors.QuestVisitor;
import com.martelstudios.hyquests.extension.count.CountQuest;

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
