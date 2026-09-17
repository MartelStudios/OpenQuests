package com.martelstudios.openquests.extension.quests.movement;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.protocol.MovementStates;
import com.hypixel.hytale.server.core.Message;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestProgression;

import javax.annotation.Nonnull;

public class JumpQuestProgression extends MovementQuestProgression<JumpQuestProgression> {

    public static final BuilderCodec<JumpQuestProgression> CODEC =
        BuilderCodec.builder(JumpQuestProgression.class, JumpQuestProgression::new, QuantityQuestProgression.BASE_CODEC)
                    .build();

    /**
     * Whether the player was already off the ground last time this quest was shown a sample. Held
     * per quest rather than per player: several jump quests each keep their own view of the same
     * player, which is what lets the count work without a table to clean up when they leave.
     */
    private transient boolean wasJumping;

    @Override
    public JumpQuestAsset getAsset() {
        return (JumpQuestAsset) super.getAsset();
    }

    /**
     * Jumping is a state that lasts, so the count is taken from the tick it starts on. Reads the
     * sample and remembers it, unlike the other movement quests, which only measure.
     */
    @Override
    protected double advance(@Nonnull MovementStates states, double metres) {
        boolean jumping = states.jumping;
        boolean tookOff = jumping && !wasJumping;

        wasJumping = jumping;

        return tookOff ? 1 : 0;
    }

    @Nonnull
    @Override
    public Message getDefaultTitle() {
        return Message.translation("openquests.quest.default.jump").param("quantity", getTargetQuantity());
    }
}
