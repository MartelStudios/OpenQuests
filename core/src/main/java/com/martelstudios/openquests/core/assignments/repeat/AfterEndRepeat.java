package com.martelstudios.openquests.core.assignments.repeat;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.core.models.AssignmentRecord;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Again on the next occasion once what the last one opened has ended, however it ended: done,
 * failed or given up. The quest's own constraints still decide whether a player takes it.
 */
public class AfterEndRepeat extends AssignmentRepeat {

    public static final String TYPE = "AfterEnd";

    public static final BuilderCodec<AfterEndRepeat> CODEC = BuilderCodec.builder(AfterEndRepeat.class, AfterEndRepeat::new, AssignmentRepeat.BASE_CODEC).build();

    @Nonnull
    @Override
    protected Decision decideWithin(@Nullable AssignmentRecord record, @Nonnull String occasion, boolean timed, boolean seen, boolean lineRunning) {
        if (isSamePeriod(record, occasion, timed) || lineRunning) return Decision.SKIP;
        return Decision.HAND_OUT;
    }
}
