package com.martelstudios.opendialogues.characters.filters.impl;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.opendialogues.characters.filters.FilterContext;

import javax.annotation.Nonnull;
import java.util.Arrays;

/**
 * Holds for NPCs whose role belongs to one of the listed NPC groups.
 */
public class OpenDialoguesGroupFilter extends ListFilter {

    public static final String TYPE = "Group";

    public static final BuilderCodec<OpenDialoguesGroupFilter> CODEC = codec(OpenDialoguesGroupFilter.class, OpenDialoguesGroupFilter::new, "Groups");

    protected OpenDialoguesGroupFilter() {}

    @Override
    protected boolean evaluate(@Nonnull FilterContext context) {
        return Arrays.stream(values).anyMatch(context.inGroup());
    }
}
