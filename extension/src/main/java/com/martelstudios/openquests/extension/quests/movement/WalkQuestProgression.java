package com.martelstudios.openquests.extension.quests.movement;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.protocol.MovementStates;
import com.hypixel.hytale.server.core.Message;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestProgression;

import javax.annotation.Nonnull;

public class WalkQuestProgression extends MovementQuestProgression<WalkQuestProgression> {

    public static final BuilderCodec<WalkQuestProgression> CODEC =
        BuilderCodec.builder(WalkQuestProgression.class, WalkQuestProgression::new, QuantityQuestProgression.BASE_CODEC)
                    .build();

    @Override
    public WalkQuestAsset getAsset() {
        return (WalkQuestAsset) super.getAsset();
    }

    /**
     * The slow pace a player has to hold a key down for, not the one they travel at by default —
     * so this is a quest about taking one's time rather than about covering ground.
     */
    @Override
    protected double advance(@Nonnull MovementStates states, double metres) {
        return states.walking && states.onGround ? metres : 0;
    }

    @Nonnull
    @Override
    public Message getDefaultTitle() {
        return Message.translation("openquests.quest.default.walk").param("quantity", getTargetQuantity());
    }
}
