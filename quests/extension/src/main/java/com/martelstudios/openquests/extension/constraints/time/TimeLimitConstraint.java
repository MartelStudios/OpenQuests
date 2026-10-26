package com.martelstudios.openquests.extension.constraints.time;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;

/**
 * Gives every quest made from the asset a set time, counted from the moment it started. A shared
 * quest is timed from its own start, whenever each player joined it.
 */
public class TimeLimitConstraint extends ExpiringConstraint {

    public static final BuilderCodec<TimeLimitConstraint> CODEC = BuilderCodec.builder(TimeLimitConstraint.class, TimeLimitConstraint::new, ExpiringConstraint.BASE_CODEC)
                                                                              .append(new KeyedCodec<>("Seconds", Codec.DURATION_SECONDS, true), (constraint, duration) -> constraint.duration = duration, constraint -> constraint.duration)
                                                                              .addValidator(Validators.nonNull())
                                                                              .addValidator(Validators.greaterThan(Duration.ZERO))
                                                                              .add()
                                                                              .build();

    protected Duration duration;

    protected TimeLimitConstraint() {}

    /**
     * A quest that never said when it started is left untimed, rather than timed out on the spot.
     */
    @Nullable
    @Override
    public Instant getDeadline(@Nonnull AbstractQuestProgression<?> quest) {
        Instant startedAt = quest.getStartedAt();
        return startedAt == null ? null : startedAt.plus(duration);
    }

    /**
     * @return how long a quest has from the moment it started.
     */
    @Nonnull
    public Duration getDuration() {
        return duration;
    }
}
