package com.martelstudios.opendialogues.characters;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.opendialogues.characters.filters.CharacterFilter;
import com.martelstudios.opendialogues.characters.filters.FilterContext;

import javax.annotation.Nonnull;

/**
 * A third-party filter that always fails, to check one broken filter never stops a draw.
 */
public class ThrowingFilter extends CharacterFilter {

    public static final String TYPE = "TestThrowing";

    public static final BuilderCodec<ThrowingFilter> CODEC = BuilderCodec.builder(ThrowingFilter.class, ThrowingFilter::new, CharacterFilter.BASE_CODEC).build();

    @Override
    protected boolean evaluate(@Nonnull FilterContext context) {
        throw new IllegalStateException("broken filter");
    }
}
