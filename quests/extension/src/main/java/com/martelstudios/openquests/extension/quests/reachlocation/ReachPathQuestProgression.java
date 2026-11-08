package com.martelstudios.openquests.extension.quests.reachlocation;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.Message;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestProgression;

import javax.annotation.Nonnull;

/**
 * Counts the spots of a path reached in order: the count is the index of the next spot.
 */
public class ReachPathQuestProgression extends QuantityQuestProgression<ReachPathQuestProgression> {

    public static final BuilderCodec<ReachPathQuestProgression> CODEC =
        BuilderCodec.builder(ReachPathQuestProgression.class, ReachPathQuestProgression::new, QuantityQuestProgression.BASE_CODEC)
                    .build();

    protected ReachPathQuestProgression() {}

    @Override
    public ReachPathQuestAsset getAsset() {
        return (ReachPathQuestAsset) super.getAsset();
    }

    @Nonnull
    @Override
    public Message getDefaultTitle() {
        return Message.translation("openquests.quest.default.reach-path").param("quantity", getTargetQuantity());
    }
}
