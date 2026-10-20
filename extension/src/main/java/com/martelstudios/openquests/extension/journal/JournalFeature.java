package com.martelstudios.openquests.extension.journal;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.permissions.provider.HytalePermissionsProvider;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.opennavigation.events.RouteChangedEvent;
import com.martelstudios.openquests.core.OpenQuestsServices;
import com.martelstudios.openquests.extension.journal.commands.JournalCommand;
import com.martelstudios.openquests.extension.journal.navigation.JournalRoutes;
import com.martelstudios.openquests.extension.track.QuestTrackService;

import javax.annotation.Nonnull;

/**
 * The quest journal. A presentation choice rather than part of the quest system, so a server
 * wanting its own UI simply leaves this one out.
 */
public final class JournalFeature {

    /**
     * Lets a player lay chapters out from the journal's graph, writing their files. Under the
     * plugin's own base permission, the way its commands' permissions are named.
     */
    public static final String EDIT_PERMISSION = "martelstudios.openquests.journal.edit";

    private JournalFeature() {}

    public static void register(@Nonnull JavaPlugin plugin, @Nonnull OpenQuestsServices services, @Nonnull QuestTrackService tracks) {
        // Granted the way world editing commands are; the groups are rebuilt once already at boot
        PermissionsModule.registerPermission(EDIT_PERMISSION, HytalePermissionsProvider.GROUP_WORLD_EDITOR);
        PermissionsModule.get().refreshVirtualGroups();

        plugin.getCommandRegistry().registerCommand(new JournalCommand());
        plugin.getEntityStoreRegistry().registerSystem(new QuestPageTickingSystem());
        plugin.getEventRegistry()
              .register(RouteChangedEvent.class, JournalRoutes.NAMESPACE, event -> handleRouteChanged(event, services, tracks));
    }

    /**
     * Redraws the page the player already has open rather than sending a new one: opening a custom
     * page dismisses whatever stood there, and the client would lose the scroll with it.
     */
    private static void handleRouteChanged(@Nonnull RouteChangedEvent event, @Nonnull OpenQuestsServices services, @Nonnull QuestTrackService tracks) {
        Ref<EntityStore> reference = event.getReference();
        Store<EntityStore> store = reference.getStore();

        Player player = store.getComponent(reference, Player.getComponentType());
        if (player == null) return;

        var pageManager = player.getPageManager();

        if (pageManager.getCustomPage() instanceof QuestPage page) {
            page.navigatedTo(reference, event.getRouter(), event.getRoute());
            return;
        }

        pageManager.openCustomPage(reference, store, new QuestPage(services, tracks, event.getPlayer(), event.getRouter(), event.getRoute()));
    }
}
