package com.martelstudios.openquests.extension.journal.navigation;

import com.martelstudios.opennavigation.routes.Route;
import com.martelstudios.openquests.extension.journal.navigation.routes.JournalRoute;
import com.martelstudios.openquests.extension.journal.navigation.routes.QuestListRoute;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Where the journal's routes are built. A route holds a place in one player's history and is
 * rewritten as they move, so every one of these is a fresh object: handing the same instance to two
 * players would have the second tear the first's history apart.
 */
public final class JournalRoutes {

    /** The dispatch key: this mod hears about these routes and no others. */
    public static final String NAMESPACE = "openquests";

    public static final String JOURNAL = "journal";
    public static final String QUEST = "quest";

    public static final String TAB_ACTIVE = "journal/active";
    public static final String TAB_DONE = "journal/done";
    public static final String TAB_ALL = "journal/all";

    /** Declaration order, which is the order the tab buttons are drawn in. */
    private static final List<String> TABS = List.of(TAB_ACTIVE, TAB_DONE, TAB_ALL);

    private JournalRoutes() {}

    /**
     * @param openTab which list stands open, {@link #TAB_ACTIVE} for a journal being opened afresh.
     */
    @Nonnull
    public static JournalRoute journal(@Nonnull String openTab) {
        List<Route> tabs = TABS.stream().map(name -> (Route) new QuestListRoute(name)).toList();

        return new JournalRoute(tabs, new QuestListRoute(openTab));
    }

    @Nonnull
    public static List<String> getTabNames() {
        return TABS;
    }
}
