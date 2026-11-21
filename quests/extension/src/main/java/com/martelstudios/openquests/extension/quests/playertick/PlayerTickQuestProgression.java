package com.martelstudios.openquests.extension.quests.playertick;

import com.hypixel.hytale.protocol.MovementStates;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestProgression;

import javax.annotation.Nonnull;

/**
 * Runtime state shared by quests that count player tick samples. Subtypes decide what one
 * sample is worth; the counter and the completion check come from {@link QuantityQuestProgression}.
 *
 * @param <Q> the concrete quest type extending this class
 */
public abstract class PlayerTickQuestProgression<Q extends PlayerTickQuestProgression<Q>> extends QuantityQuestProgression<Q> {

    /**
     * The fractional progress toward the next whole unit. This remainder resets on restart.
     */
    private transient double pending;

    /**
     * @param metres  how far the player travelled horizontally during this sample, zero while still.
     * @param seconds how long the sample lasted.
     * @return what this sample is worth in counted units, fractional for a distance or a duration
     * and never negative. Called once per tick per player holding the quest, so it stays a comparison.
     */
    protected abstract double advance(@Nonnull MovementStates states, double metres, double seconds);

    /**
     * Adds a sample to what the samples are worth, keeping what is left of a unit for the next one:
     * a counter moving by whole steps is what the panel and the journal are able to draw.
     *
     * @return the whole units the sample completes, for the caller to count.
     */
    public int accumulate(@Nonnull MovementStates states, double metres, double seconds) {
        double advance = advance(states, metres, seconds);
        if (advance <= 0) return 0;

        pending += advance;

        int whole = (int) pending;
        pending -= whole;
        return whole;
    }

    /**
     * Movement only ever brings the quest to its end, never back from one.
     */
    @Override
    protected void settle() {
        if (checkCompletion()) new SetState(QuestState.SUCCESSFUL).applyTo(this);
    }
}
