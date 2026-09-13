package com.martelstudios.openquests.extension.journal;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.opennavigation.events.RouteChangedEvent;
import com.martelstudios.openquests.extension.journal.commands.JournalCommand;
import com.martelstudios.openquests.extension.journal.navigation.JournalRoutes;

import javax.annotation.Nonnull;

/**
 * The quest journal. A presentation choice rather than part of the quest system, so a server
 * wanting its own UI simply leaves this one out.
 */
public final class JournalFeature {

    private JournalFeature() {}

    public static void register(@Nonnull JavaPlugin plugin) {
        plugin.getCommandRegistry().registerCommand(new JournalCommand());
        plugin.getEventRegistry()
              .register(RouteChangedEvent.class, JournalRoutes.NAMESPACE, JournalFeature::handleRouteChanged);
    }

    /**
     * Redraws the page the player already has open rather than sending a new one: opening a custom
     * page dismisses whatever stood there, and the client would lose the scroll with it.
     */
    private static void handleRouteChanged(@Nonnull RouteChangedEvent event) {
        Ref<EntityStore> reference = event.getReference();
        Store<EntityStore> store = reference.getStore();

        Player player = store.getComponent(reference, Player.getComponentType());
        if (player == null) return;

        var pageManager = player.getPageManager();

        if (pageManager.getCustomPage() instanceof QuestPage page) {
            page.navigatedTo(reference, event.getRoute());
            return;
        }

        pageManager.openCustomPage(reference, store, new QuestPage(event.getPlayer(), event.getRoute()));
    }
}
