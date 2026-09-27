package com.martelstudios.openquests.extension.quests.movement;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.protocol.MovementStates;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestProgression;

import javax.annotation.Nonnull;

public class RunQuestProgression extends TravelQuestProgression<RunQuestProgression> {

    public static final BuilderCodec<RunQuestProgression> CODEC =
        BuilderCodec.builder(RunQuestProgression.class, RunQuestProgression::new, QuantityQuestProgression.BASE_CODEC)
                    .build();

    @Override
    public RunQuestAsset getAsset() {
        return (RunQuestAsset) super.getAsset();
    }

    /**
     * The pace a player travels at without asking for anything, which makes this the one a quest
     * about covering ground wants. Sprinting is a state of its own and does not count here.
     */
    @Override
    protected boolean isAtPace(@Nonnull MovementStates states) {
        return states.running && states.onGround;
    }

    @Nonnull
    @Override
    protected String getPaceKey() {
        return "run";
    }
}
