package com.martelstudios.opendialogues.characters.filters.impl;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.opendialogues.characters.filters.FilterContext;

import javax.annotation.Nonnull;

/**
 * Holds where the NPC stands in one of the listed biomes.
 */
public class OpenDialoguesBiomeFilter extends ListFilter {

    public static final String TYPE = "Biome";

    public static final BuilderCodec<OpenDialoguesBiomeFilter> CODEC = codec(OpenDialoguesBiomeFilter.class, OpenDialoguesBiomeFilter::new, "Biomes");

    protected OpenDialoguesBiomeFilter() {}

    @Override
    protected boolean evaluate(@Nonnull FilterContext context) {
        return lists(context.biome());
    }
}
