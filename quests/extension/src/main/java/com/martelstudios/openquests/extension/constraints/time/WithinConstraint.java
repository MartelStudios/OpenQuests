package com.martelstudios.openquests.extension.constraints.time;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.martelstudios.openquests.core.constraints.QuestConstraint;

import javax.annotation.Nonnull;
import java.time.Duration;

/**
 * Turns a counted quest into a combo: the target is to be reached within a window opened by the
 * first action counted, and a window closing short restarts the count. The quest itself never fails.
 */
public class WithinConstraint extends QuestConstraint {

    public static final BuilderCodec<WithinConstraint> CODEC = BuilderCodec.builder(WithinConstraint.class, WithinConstraint::new, QuestConstraint.BASE_CODEC)
                                                                           .append(new KeyedCodec<>("Seconds", Codec.DURATION_SECONDS, true), (constraint, window) -> constraint.window = window, constraint -> constraint.window)
                                                                           .addValidator(Validators.nonNull())
                                                                           .addValidator(Validators.greaterThan(Duration.ZERO))
                                                                           .add()
                                                                           .build();

    protected Duration window;

    protected WithinConstraint() {}

    /**
     * @return how long a combo lasts from its first action.
     */
    @Nonnull
    public Duration getWindow() {
        return window;
    }
}
