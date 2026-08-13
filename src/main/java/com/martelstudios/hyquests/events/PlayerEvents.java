package com.martelstudios.hyquests.events;

import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.martelstudios.hyquests.stores.QuestStoreComponent;

public class PlayerEvents {
    /**
     * Pulls the player's own quests into the datastore. Scope services assign theirs separately,
     * each on the event that concerns it.
     */
    public static void handlePlayerConnectEvent(PlayerConnectEvent playerConnectEvent) {
        playerConnectEvent.getHolder()
                          .ensureAndGetComponent(QuestStoreComponent.getComponentType())
                          .loadQuests();
    }
}
