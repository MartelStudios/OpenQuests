package com.martelstudios.opendialogues.characters.filters.impl;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.martelstudios.opendialogues.characters.filters.CharacterFilter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.function.Supplier;

/**
 * A filter holding when one value of the NPC is among those listed.
 */
abstract class ListFilter extends CharacterFilter {

    protected String[] values = new String[0];

    @Nonnull
    static <T extends ListFilter> BuilderCodec<T> codec(@Nonnull Class<T> filterClass, @Nonnull Supplier<T> constructor, @Nonnull String key) {
        return BuilderCodec.builder(filterClass, constructor, CharacterFilter.BASE_CODEC)
                           .append(new KeyedCodec<>(key, new ArrayCodec<>(Codec.STRING, String[]::new)), (filter, values) -> filter.values = values, filter -> filter.values)
                           .add()
                           .build();
    }

    protected boolean lists(@Nullable String value) {
        return value != null && Arrays.asList(values).contains(value);
    }
}
