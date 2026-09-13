package com.martelstudios.openquests.extension.journal.navigation.routes;

import com.hypixel.hytale.server.core.Message;

import javax.annotation.Nonnull;

/**
 * A route the journal can name in its trail. OpenNavigation says where a player is, never what to
 * call it — a label is a look, and looks belong here.
 */
public interface LabelledRoute {

    @Nonnull
    Message getLabel();
}
