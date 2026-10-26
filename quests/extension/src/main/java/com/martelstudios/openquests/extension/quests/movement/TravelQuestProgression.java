package com.martelstudios.openquests.extension.quests.movement;

import com.hypixel.hytale.protocol.MovementStates;
import com.hypixel.hytale.server.core.Message;
import com.martelstudios.openquests.extension.quests.movement.TravelQuestAsset.Measure;

import javax.annotation.Nonnull;

/**
 * Runtime state shared by the quests counted at one pace. Subtypes only say which pace; the
 * measure the asset picks decides whether a sample is worth its distance or its duration.
 *
 * @param <Q> the concrete quest type extending this class
 */
public abstract class TravelQuestProgression<Q extends TravelQuestProgression<Q>> extends MovementQuestProgression<Q> {

    /**
     * @return whether the player moves at this quest's pace. Called once per tick per player
     * holding the quest, so it stays a comparison.
     */
    protected abstract boolean isAtPace(@Nonnull MovementStates states);

    /**
     * @return the pace as the default titles name it, {@code "run"} for
     * {@code openquests.quest.default.run}.
     */
    @Nonnull
    protected abstract String getPaceKey();

    @Override
    public TravelQuestAsset getAsset() {
        return (TravelQuestAsset) super.getAsset();
    }

    @Override
    protected double advance(@Nonnull MovementStates states, double metres, double seconds) {
        return isAtPace(states) ? getAsset().getMeasure().of(metres, seconds) : 0;
    }

    @Nonnull
    @Override
    public Message getDefaultTitle() {
        String suffix = getAsset().getMeasure() == Measure.SECONDS ? "-for" : "";

        return Message.translation("openquests.quest.default." + getPaceKey() + suffix).param("quantity", getTargetQuantity());
    }
}
