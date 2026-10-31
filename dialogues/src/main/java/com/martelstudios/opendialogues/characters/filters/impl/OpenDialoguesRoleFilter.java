package com.martelstudios.opendialogues.characters.filters.impl;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.opendialogues.characters.filters.FilterContext;

import javax.annotation.Nonnull;

/**
 * Holds for NPCs wearing one of the listed roles.
 */
public class OpenDialoguesRoleFilter extends ListFilter {

    public static final String TYPE = "Role";

    public static final BuilderCodec<OpenDialoguesRoleFilter> CODEC = codec(OpenDialoguesRoleFilter.class, OpenDialoguesRoleFilter::new, "Roles");

    protected OpenDialoguesRoleFilter() {}

    @Override
    protected boolean evaluate(@Nonnull FilterContext context) {
        return lists(context.role());
    }
}
