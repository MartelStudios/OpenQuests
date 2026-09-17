package com.martelstudios.openquests.extension.hud;

import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.events.QuestStateChangedEvent;
import com.martelstudios.openquests.core.events.QuestUpdatedEvent;
import com.martelstudios.openquests.core.scopes.player.events.QuestAddedToPlayerStoreEvent;
import com.martelstudios.openquests.core.scopes.player.events.QuestRemovedFromPlayerStoreEvent;
import com.martelstudios.openquests.extension.track.events.QuestTrackedEvent;
import com.martelstudios.openquests.extension.track.events.QuestUntrackedEvent;

import javax.annotation.Nonnull;

/**
 * The tracker panel listing a player's in-progress quests. A presentation choice rather than part
 * of the quest system, so a server wanting its own UI simply leaves this one out.
 */
public final class HudFeature {

    private HudFeature() {}

    public static void register(@Nonnull JavaPlugin plugin) {
        plugin.getEntityStoreRegistry().registerSystem(new QuestHudTickingSystem());

        plugin.getEventRegistry().registerGlobal(PlayerConnectEvent.class, QuestHudRefresh::handlePlayerConnect);
        plugin.getEventRegistry().registerGlobal(QuestUpdatedEvent.class, QuestHudRefresh::handleQuestUpdated);
        plugin.getEventRegistry().registerGlobal(QuestStateChangedEvent.class, QuestHudRefresh::handleQuestStateChanged);
        plugin.getEventRegistry().registerGlobal(QuestTrackedEvent.class, QuestHudRefresh::handleQuestTracked);
        plugin.getEventRegistry().registerGlobal(QuestUntrackedEvent.class, QuestHudRefresh::handleQuestUntracked);
        plugin.getEventRegistry().registerGlobal(QuestAddedToPlayerStoreEvent.class, QuestHudRefresh::handleQuestAddedToPlayerStore);
        plugin.getEventRegistry().registerGlobal(QuestRemovedFromPlayerStoreEvent.class, QuestHudRefresh::handleQuestRemovedFromPlayerStore);
    }
}
