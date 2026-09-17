package com.martelstudios.openquests.extension.quests.movement;

import com.hypixel.hytale.protocol.MovementStates;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestProgression;

import javax.annotation.Nonnull;

/**
 * Runtime state shared by every quest counted from how the player moves. Subtypes only say what one
 * sample is worth; the counter and the completion check come from {@link QuantityQuestProgression}.
 *
 * @param <Q> the concrete quest type extending this class
 */
public abstract class MovementQuestProgression<Q extends MovementQuestProgression<Q>> extends QuantityQuestProgression<Q> {

    /**
     * What the samples have been worth without yet adding up to a whole unit. Not serialized: under
     * one metre is not worth a field in every save, and a restart losing it costs a single step.
     */
    private transient double pending;

    /**
     * @param metres how far the player travelled horizontally during this sample, zero while still.
     * @return what this sample is worth in counted units, fractional for a distance and never
     * negative. Called once per tick per player holding the quest, so it stays a comparison.
     */
    protected abstract double advance(@Nonnull MovementStates states, double metres);

    /**
     * Adds a sample to the counter, keeping what is left of a unit for the next one — a counter
     * moving by whole steps is what the panel and the journal are able to draw.
     *
     * @return {@code true} if the counter moved, which is the only thing worth writing down.
     */
    public boolean accumulate(@Nonnull MovementStates states, double metres) {
        double advance = advance(states, metres);
        if (advance <= 0) return false;

        pending += advance;

        int whole = (int) pending;
        if (whole <= 0) return false;

        pending -= whole;
        setCurrentQuantity(getCurrentQuantity() + whole);

        return true;
    }
}
