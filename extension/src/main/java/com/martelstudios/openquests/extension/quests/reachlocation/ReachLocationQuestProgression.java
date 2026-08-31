package com.martelstudios.openquests.extension.quests.reachlocation;

import com.hypixel.hytale.server.core.Message;

import javax.annotation.Nonnull;
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

    @Nonnull
    @Override
    public Message getDefaultTitle() {
        var asset = getAsset();
        if (asset == null || asset.getPosition() == null) return super.getDefaultTitle();

        var position = asset.getPosition();
        return Message.translation("openquests.quest.default.reach-location")
                      .param("x", position.x())
                      .param("y", position.y())
                      .param("z", position.z());
    }
}
