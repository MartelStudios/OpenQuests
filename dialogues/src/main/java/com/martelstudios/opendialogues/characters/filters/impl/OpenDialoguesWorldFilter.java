package com.martelstudios.opendialogues.characters.filters.impl;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.opendialogues.characters.filters.FilterContext;

import javax.annotation.Nonnull;

/**
 * Holds in the listed worlds and instances, by world name.
 */
public class OpenDialoguesWorldFilter extends ListFilter {

    public static final String TYPE = "World";

    public static final BuilderCodec<OpenDialoguesWorldFilter> CODEC = codec(OpenDialoguesWorldFilter.class, OpenDialoguesWorldFilter::new, "Worlds");

    protected OpenDialoguesWorldFilter() {}

    @Override
    protected boolean evaluate(@Nonnull FilterContext context) {
        return lists(context.world());
    }
}
