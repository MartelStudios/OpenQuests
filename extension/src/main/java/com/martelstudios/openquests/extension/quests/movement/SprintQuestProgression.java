package com.martelstudios.openquests.extension.quests.movement;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.protocol.MovementStates;
import com.hypixel.hytale.server.core.Message;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestProgression;

import javax.annotation.Nonnull;

public class SprintQuestProgression extends MovementQuestProgression<SprintQuestProgression> {

    public static final BuilderCodec<SprintQuestProgression> CODEC =
        BuilderCodec.builder(SprintQuestProgression.class, SprintQuestProgression::new, QuantityQuestProgression.BASE_CODEC)
                    .build();

    @Override
    public SprintQuestAsset getAsset() {
        return (SprintQuestAsset) super.getAsset();
    }

    /**
     * The fastest pace, held down for as long as it lasts — so a quest counting it is a quest about
     * keeping it up rather than about the ground covered.
     */
    @Override
    protected double advance(@Nonnull MovementStates states, double metres) {
        return states.sprinting && states.onGround ? metres : 0;
    }

    @Nonnull
    @Override
    public Message getDefaultTitle() {
        return Message.translation("openquests.quest.default.sprint").param("quantity", getTargetQuantity());
    }
}
