package com.martelstudios.openquests.core.assignments.trigger;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.martelstudios.openquests.core.assignments.Occasion;
import com.martelstudios.openquests.core.utils.EntityComponents;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.UUID;

/**
 * On a date, or from it on every period: {@code At} is the first, an ISO date and time with its
 * offset, {@code Every} an ISO duration. Each period is one occasion, handed to the holders online
 * as it begins and to those reached while it lasts, a player as they connect: nobody is handed a
 * period they were away for.
 */
public class ScheduleTrigger extends AssignmentTrigger {

    public static final String TYPE = "Schedule";

    public static final BuilderCodec<ScheduleTrigger> CODEC = BuilderCodec.builder(ScheduleTrigger.class, ScheduleTrigger::new, AssignmentTrigger.BASE_CODEC)
                                                                          .append(new KeyedCodec<>("At", Codec.STRING), ScheduleTrigger::setAt, trigger -> trigger.at)
                                                                          .addValidator(Validators.nonNull())
                                                                          .add()
                                                                          .append(new KeyedCodec<>("Every", Codec.STRING), ScheduleTrigger::setEvery, trigger -> trigger.every)
                                                                          .add()
                                                                          .build();

    private String at;

    @Nullable
    private String every;

    @Nullable
    private Instant start;

    @Nullable
    private Duration period;

    @Nullable
    private String error;

    public ScheduleTrigger() {}

    public ScheduleTrigger(@Nonnull String at, @Nullable String every) {
        setAt(this, at);
        setEvery(this, every);
    }

    /**
     * @return the start of the period that moment falls in, {@code null} before the first one.
     * Without {@code Every}, the one date, which lasts for good once reached.
     */
    @Nullable
    public Instant currentPeriod(@Nonnull Instant now) {
        if (start == null || now.isBefore(start)) return null;
        if (period == null) return start;

        long elapsed = Duration.between(start, now).toMillis() / period.toMillis();
        return start.plus(period.multipliedBy(elapsed));
    }

    @Nullable
    @Override
    public Occasion onConnect(@Nonnull UUID playerId, @Nonnull EntityComponents player) {
        Instant current = currentPeriod(Instant.now());
        return current == null ? null : Occasion.connection(playerId, player).during(current.toString());
    }

    @Nullable
    @Override
    public Occasion onTick(@Nonnull Instant now) {
        Instant current = currentPeriod(now);
        return current == null ? null : Occasion.period(current.toString());
    }

    @Nullable
    @Override
    public String findInconsistency() {
        if (error != null) return error;
        if (period != null && (period.isZero() || period.isNegative())) return "Every must be a positive duration";
        return null;
    }

    private static void setAt(@Nonnull ScheduleTrigger trigger, @Nullable String at) {
        trigger.at = at;
        if (at == null) return;

        try {
            trigger.start = OffsetDateTime.parse(at).toInstant();
        } catch (DateTimeParseException e) {
            trigger.error = "At is not an ISO date and time with its offset: " + at;
        }
    }

    private static void setEvery(@Nonnull ScheduleTrigger trigger, @Nullable String every) {
        trigger.every = every;
        if (every == null) return;

        try {
            trigger.period = Duration.parse(every);
        } catch (DateTimeParseException e) {
            trigger.error = "Every is not an ISO duration: " + every;
        }
    }
}
