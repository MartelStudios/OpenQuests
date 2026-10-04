package com.martelstudios.openquests.core.assignments.repeat;

import com.hypixel.hytale.codec.builder.BuilderCodec;

import javax.annotation.Nonnull;

/**
 * Once per occasion and holder, the default: once for good on connection, once per world entered,
 * once per period.
 */
public class OnceRepeat extends AssignmentRepeat {

    public static final String TYPE = "Once";

    public static final BuilderCodec<OnceRepeat> CODEC = BuilderCodec.builder(OnceRepeat.class, OnceRepeat::new, AssignmentRepeat.BASE_CODEC).build();

    @Nonnull
    @Override
    protected Decision decideWithin(@Nonnull AssignmentHistory history) {
        return history.isSeen() ? Decision.SKIP : Decision.HAND_OUT;
    }
}
