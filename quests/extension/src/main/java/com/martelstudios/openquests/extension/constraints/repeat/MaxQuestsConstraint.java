package com.martelstudios.openquests.extension.constraints.repeat;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.martelstudios.openquests.core.constraints.AssignmentRequest;
import com.martelstudios.openquests.core.constraints.QuestConstraint;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.models.QuestStates;
import com.martelstudios.openquests.core.models.QuestTally;

import javax.annotation.Nonnull;

/**
 * Stops handing quests from the asset out to a player once that many of theirs stand in the
 * listed states. Every state counts unless the asset lists some: running quests for one at a time,
 * successes for a number of wins, failures and give-ups for a number of misses.
 */
public class MaxQuestsConstraint extends QuestConstraint {

    public static final BuilderCodec<MaxQuestsConstraint> CODEC = BuilderCodec.builder(MaxQuestsConstraint.class, MaxQuestsConstraint::new, QuestConstraint.BASE_CODEC)
                                                                              .append(new KeyedCodec<>("Count", Codec.INTEGER, true), (constraint, count) -> constraint.count = count, constraint -> Integer.valueOf(constraint.count))
                                                                              .addValidator(Validators.min(1))
                                                                              .add()
                                                                              .append(new KeyedCodec<>("States", QuestStates.CODEC), (constraint, states) -> constraint.states = states, constraint -> constraint.states)
                                                                              .addValidator(Validators.nonEmptyArray())
                                                                              .addValidator(Validators.uniqueInArray())
                                                                              .add()
                                                                              .build();

    protected int count;

    @Nonnull
    protected QuestState[] states = QuestStates.ALL;

    protected MaxQuestsConstraint() {}

    @Override
    public boolean allowsAssignment(@Nonnull AssignmentRequest request) {
        return getRemaining(request.tally()) > 0;
    }

    /**
     * @return how many more the player may still be handed, never below zero.
     */
    public int getRemaining(@Nonnull QuestTally tally) {
        return Math.max(0, count - tally.count(states));
    }

    /**
     * @return how many quests from the asset a player may have standing in those states.
     */
    public int getCount() {
        return count;
    }

    /**
     * @return whether only running quests count, which makes the cap a number held at once.
     */
    public boolean isAtOnce() {
        return states.length == 1 && states[0] == QuestState.IN_PROGRESS;
    }
}
