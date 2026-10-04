package com.martelstudios.openquests.core.assignments.repeat;

import com.hypixel.hytale.codec.builder.BuilderCodec;

import javax.annotation.Nonnull;

/**
 * Again on the next occasion once what the last one opened has ended, however it ended: done,
 * failed or given up. The quest's own constraints still decide whether a player takes it.
 */
public class AfterEndRepeat extends AssignmentRepeat {

    public static final String TYPE = "AfterEnd";

    public static final BuilderCodec<AfterEndRepeat> CODEC = BuilderCodec.builder(AfterEndRepeat.class, AfterEndRepeat::new, AssignmentRepeat.BASE_CODEC).build();

    @Nonnull
    @Override
    protected Decision decideWithin(@Nonnull AssignmentHistory history) {
        if (history.isSamePeriod() || history.isLineRunning()) return Decision.SKIP;
        return Decision.HAND_OUT;
    }
}
