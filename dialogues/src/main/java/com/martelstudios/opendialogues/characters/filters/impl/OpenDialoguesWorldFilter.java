package com.martelstudios.opendialogues.characters.filters.impl;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.martelstudios.opendialogues.characters.filters.CharacterFilter;
import com.martelstudios.opendialogues.characters.filters.FilterContext;
import com.martelstudios.opendialogues.characters.filters.WorldNamePattern;

import javax.annotation.Nonnull;

/**
 * Holds in the listed worlds and instances, each entry a regular expression on the whole world name,
 * since instance copies carry a generated suffix.
 */
public class OpenDialoguesWorldFilter extends CharacterFilter {

    public static final String TYPE = "World";

    public static final BuilderCodec<OpenDialoguesWorldFilter> CODEC = BuilderCodec.builder(OpenDialoguesWorldFilter.class, OpenDialoguesWorldFilter::new, CharacterFilter.BASE_CODEC)
                                                                                   .append(new KeyedCodec<>("Worlds", new ArrayCodec<>(WorldNamePattern.CODEC, WorldNamePattern[]::new)), (filter, worlds) -> filter.worlds = worlds, filter -> filter.worlds)
                                                                                   .add()
                                                                                   .validator((filter, results) -> {
                                                                                       for (WorldNamePattern world : filter.worlds) {
                                                                                           if (world.getError() == null) continue;
                                                                                           results.fail("Worlds pattern '" + world.getSource() + "' does not compile: " + world.getError());
                                                                                           return;
                                                                                       }
                                                                                   })
                                                                                   .build();

    protected WorldNamePattern[] worlds = new WorldNamePattern[0];

    protected OpenDialoguesWorldFilter() {}

    @Override
    protected boolean evaluate(@Nonnull FilterContext context) {
        for (WorldNamePattern world : worlds) {
            if (world.matches(context.world())) return true;
        }
        return false;
    }
}
