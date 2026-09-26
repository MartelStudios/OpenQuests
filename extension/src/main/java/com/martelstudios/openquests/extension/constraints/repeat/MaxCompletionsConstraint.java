package com.martelstudios.openquests.extension.constraints.repeat;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.martelstudios.openquests.core.constraints.QuestConstraint;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import com.martelstudios.openquests.core.models.QuestCompletions;
import com.martelstudios.openquests.core.models.QuestState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.util.UUID;

/**
 * Stops handing quests from the asset out to a player once enough of them ended that way. Only
 * successes count unless the asset lists other outcomes, so failing a quest does not use up a try.
 */
public class MaxCompletionsConstraint extends QuestConstraint {

    private static final QuestState[] SUCCESSES = {QuestState.SUCCESSFUL};

    public static final BuilderCodec<MaxCompletionsConstraint> CODEC = BuilderCodec.builder(MaxCompletionsConstraint.class, MaxCompletionsConstraint::new, QuestConstraint.BASE_CODEC)
                                                                                   .append(new KeyedCodec<>("Count", Codec.INTEGER, true), (constraint, count) -> constraint.count = count, constraint -> Integer.valueOf(constraint.count))
                                                                                   .addValidator(Validators.min(1))
                                                                                   .add()
                                                                                   .append(new KeyedCodec<>("Outcomes", new ArrayCodec<>(new EnumCodec<>(QuestState.class), QuestState[]::new)), (constraint, outcomes) -> constraint.outcomes = outcomes, constraint -> constraint.outcomes)
                                                                                   .addValidator(Validators.nonEmptyArray())
                                                                                   .addValidator(Validators.uniqueInArray())
                                                                                   .add()
                                                                                   .build();

    protected int count;

    @Nonnull
    protected QuestState[] outcomes = SUCCESSES;

    protected MaxCompletionsConstraint() {}

    @Override
    public boolean allowsAssignment(@Nonnull OpenQuestAsset asset, @Nonnull UUID playerId, @Nonnull QuestCompletions completions, @Nonnull Instant now) {
        return getRemaining(completions) > 0;
    }

    @Nullable
    @Override
    public String validate(@Nonnull OpenQuestAsset asset) {
        for (QuestState outcome : outcomes) {
            if (outcome == QuestState.IN_PROGRESS) return "Outcomes can only list outcomes, not InProgress";
        }
        return null;
    }

    /**
     * @return how many more the player may still end that way, never below zero.
     */
    public int getRemaining(@Nonnull QuestCompletions completions) {
        int counted = 0;
        for (QuestState outcome : outcomes) {
            counted += completions.count(outcome);
        }
        return Math.max(0, count - counted);
    }

    /**
     * @return how many quests from the asset a player may end that way, in all.
     */
    public int getCount() {
        return count;
    }
}
