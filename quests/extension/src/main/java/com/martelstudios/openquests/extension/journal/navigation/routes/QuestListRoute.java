package com.martelstudios.openquests.extension.journal.navigation.routes;

import com.hypixel.hytale.server.core.Message;
import com.martelstudios.opennavigation.routes.StackRoute;
import com.martelstudios.openquests.extension.journal.navigation.JournalRoutes;

import javax.annotation.Nonnull;

/**
 * The list a tab of the journal opens on, and the root of everything reached from it. One per tab:
 * each keeps a history of its own, so leaving a tab and coming back lands where the player was
 * rather than at the top.
 */
public class QuestListRoute extends StackRoute implements LabelledRoute {

    public QuestListRoute(@Nonnull String name) {
        super(JournalRoutes.NAMESPACE, name);
    }

    /**
     * @return the same word whichever tab this is. Which list is showing is said by the tab buttons
     * under the trail; saying it again in the trail would read as a place of its own.
     */
    @Nonnull
    @Override
    public Message getLabel() {
        return Message.translation("openquests.page.title");
    }
}
