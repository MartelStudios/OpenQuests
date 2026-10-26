package com.martelstudios.openquests.core.assignments.repeat;

import com.hypixel.hytale.codec.builder.BuilderCodec;

import javax.annotation.Nonnull;

/**
 * Anew on every occasion: what the last one opened, if still running, fails as the new quest is
 * handed out. A period is still handed out once, however often its holder is reached during it.
 */
public class ReplaceRepeat extends AssignmentRepeat {

    public static final String TYPE = "Replace";

    public static final BuilderCodec<ReplaceRepeat> CODEC = BuilderCodec.builder(ReplaceRepeat.class, ReplaceRepeat::new, AssignmentRepeat.BASE_CODEC).build();

    @Nonnull
    @Override
    protected Decision decideWithin(@Nonnull AssignmentHistory history) {
        if (history.isSamePeriod()) return Decision.SKIP;
        return history.isLineRunning() ? Decision.REPLACE : Decision.HAND_OUT;
    }
}
