package com.martelstudios.openquests.extension.constraints.repeat;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.martelstudios.openquests.core.constraints.QuestConstraint;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import com.martelstudios.openquests.core.models.QuestCompletions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Hands a quest from the asset out at most once per period: a day's cooldown is a quest a player
 * can take at most once a day, not one they are given every day. Counted from when the last one
 * ended, whichever way, or from when it started.
 */
public class CooldownConstraint extends QuestConstraint {

    public static final BuilderCodec<CooldownConstraint> CODEC = BuilderCodec.builder(CooldownConstraint.class, CooldownConstraint::new, QuestConstraint.BASE_CODEC)
                                                                             .append(new KeyedCodec<>("Seconds", Codec.DURATION_SECONDS, true), (constraint, duration) -> constraint.duration = duration, constraint -> constraint.duration)
                                                                             .addValidator(Validators.nonNull())
                                                                             .addValidator(Validators.greaterThan(Duration.ZERO))
                                                                             .add()
                                                                             .append(new KeyedCodec<>("From", new EnumCodec<>(From.class)), (constraint, from) -> constraint.from = from, constraint -> constraint.from)
                                                                             .add()
                                                                             .build();

    protected Duration duration;

    @Nonnull
    protected From from = From.COMPLETION;

    protected CooldownConstraint() {}

    @Override
    public boolean allowsAssignment(@Nonnull OpenQuestAsset asset, @Nonnull UUID playerId, @Nonnull QuestCompletions completions, @Nonnull Instant now) {
        Instant availableAt = getAvailableAt(completions);
        return availableAt == null || !now.isBefore(availableAt);
    }

    /**
     * @return when the player may be handed the next one, {@code null} if they may right away
     * because none ever ended for them.
     */
    @Nullable
    public Instant getAvailableAt(@Nonnull QuestCompletions completions) {
        Instant last = from == From.START ? completions.getLastStartedAt() : completions.getLastCompletedAt();
        return last == null ? null : last.plus(duration);
    }

    /**
     * @return how long the player waits between two.
     */
    @Nonnull
    public Duration getDuration() {
        return duration;
    }

    /**
     * Where the wait is counted from.
     */
    public enum From {
        /**
         * The end of the last one, whichever way it ended: giving up does not skip the wait.
         */
        COMPLETION,

        /**
         * The start of the last one, so a player who is quick gets the next sooner.
         */
        START
    }
}
