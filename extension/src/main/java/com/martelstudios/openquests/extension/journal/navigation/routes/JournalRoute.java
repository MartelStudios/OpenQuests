package com.martelstudios.openquests.extension.journal.navigation.routes;

import com.hypixel.hytale.server.core.Message;
import com.martelstudios.opennavigation.routes.Route;
import com.martelstudios.opennavigation.routes.TabRoute;
import com.martelstudios.openquests.extension.journal.navigation.JournalRoutes;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * The journal itself, holding one history per tab. Switching tab is not going anywhere: it puts the
 * player back where they were in that tab, which is what having several of them is for.
 */
public class JournalRoute extends TabRoute implements LabelledRoute {

    public JournalRoute(@Nonnull List<Route> tabs, @Nonnull Route openTab) {
        super(JournalRoutes.NAMESPACE, JournalRoutes.JOURNAL, tabs, openTab);
    }

    /**
     * @return what the open tab is called. The group is not a place a player stands — it always
     * stands for whichever of its tabs is open — so it borrows that tab's name.
     */
    @Nonnull
    @Override
    public Message getLabel() {
        return getActiveTab() instanceof LabelledRoute labelled
            ? labelled.getLabel()
            : Message.raw(getActiveTab().getName());
    }

    /**
     * @return the tab standing open, named the way {@link JournalRoutes} writes them.
     */
    @Nonnull
    public String getOpenTabName() {
        return getActiveTab().getName();
    }
}
