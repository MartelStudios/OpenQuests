package com.martelstudios.openquests.extension.listener;

import com.hypixel.hytale.event.EventPriority;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.events.QuestStateChangedEvent;
import com.martelstudios.openquests.core.scopes.player.events.QuestAddedToPlayerStoreEvent;
import com.martelstudios.openquests.core.scopes.player.events.QuestRemovedFromPlayerStoreEvent;

import javax.annotation.Nonnull;

/**
 * What every quest type ticked from a system shares. Registers no quest type of its own: the
 * concrete ones declare their kind, and only the bookkeeping lives here.
 */
public final class ListenerFeature {

    private ListenerFeature() {}

    public static void register(@Nonnull JavaPlugin plugin) {
        // EventPriority.LAST, so the player's own quests are in the store and whatever connecting
        // hands out has been handed out: this pass is the one with everything to read
        plugin.getEventRegistry().registerGlobal(EventPriority.LAST, PlayerConnectEvent.class, QuestListenerService::handlePlayerConnect);

        plugin.getEventRegistry().registerGlobal(QuestAddedToPlayerStoreEvent.class, QuestListenerService::handleQuestAddedToPlayerStore);
        plugin.getEventRegistry().registerGlobal(QuestRemovedFromPlayerStoreEvent.class, QuestListenerService::handleQuestRemovedFromPlayerStore);
        plugin.getEventRegistry().registerGlobal(QuestStateChangedEvent.class, QuestListenerService::handleQuestStateChanged);
    }
}
