package com.martelstudios.openquests.core.assignments.repeat;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.core.models.AssignmentRecord;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Anew on every occasion: what the last one opened, if still running, fails as the new quest is
 * handed out. A period is still handed out once, however often its holder is reached during it.
 */
public class ReplaceRepeat extends AssignmentRepeat {

    public static final String TYPE = "Replace";

    public static final BuilderCodec<ReplaceRepeat> CODEC = BuilderCodec.builder(ReplaceRepeat.class, ReplaceRepeat::new, AssignmentRepeat.BASE_CODEC).build();

    @Nonnull
    @Override
    protected Decision decideWithin(@Nullable AssignmentRecord record, @Nonnull String occasion, boolean timed, boolean seen, boolean lineRunning) {
        if (isSamePeriod(record, occasion, timed)) return Decision.SKIP;
        return lineRunning ? Decision.REPLACE : Decision.HAND_OUT;
    }
}
