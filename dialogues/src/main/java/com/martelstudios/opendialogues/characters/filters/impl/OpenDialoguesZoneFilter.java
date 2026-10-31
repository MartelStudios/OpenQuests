package com.martelstudios.opendialogues.characters.filters.impl;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.opendialogues.characters.filters.FilterContext;

import javax.annotation.Nonnull;

/**
 * Holds in the listed map regions, as the world map names them.
 */
public class OpenDialoguesZoneFilter extends ListFilter {

    public static final String TYPE = "Zone";

    public static final BuilderCodec<OpenDialoguesZoneFilter> CODEC = codec(OpenDialoguesZoneFilter.class, OpenDialoguesZoneFilter::new, "Zones");

    protected OpenDialoguesZoneFilter() {}

    @Override
    protected boolean evaluate(@Nonnull FilterContext context) {
        return lists(context.zone());
    }
}
