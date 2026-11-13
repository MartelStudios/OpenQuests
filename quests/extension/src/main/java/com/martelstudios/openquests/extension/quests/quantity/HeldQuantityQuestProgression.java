package com.martelstudios.openquests.extension.quests.quantity;

import com.martelstudios.openquests.core.models.QuestOperation;

import javax.annotation.Nonnull;

/**
 * Runtime state of a {@link HeldQuantityQuestAsset}: the count is what the player holds, set
 * outright on every read.
 *
 * @param <Q> the concrete quest type extending this class
 */
public abstract class HeldQuantityQuestProgression<Q extends HeldQuantityQuestProgression<Q>> extends QuantityQuestProgression<Q> {

    /**
     * Sets the count outright, for a quest counting what the player holds rather than what they did.
     */
    public record Count(int quantity) implements QuestOperation<HeldQuantityQuestProgression<?>> {
        @Override
        public boolean applyTo(@Nonnull HeldQuantityQuestProgression<?> quest) {
            return quest.moveCountTo(quantity);
        }
    }
}
