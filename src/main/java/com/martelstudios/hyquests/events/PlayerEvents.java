package com.martelstudios.hyquests.events;

import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.martelstudios.hyquests.services.QuestService;
import com.martelstudios.hyquests.stores.QuestStoreComponent;

public class PlayerEvents {
    public static void handlePlayerConnectEvent(PlayerConnectEvent playerConnectEvent) {
        var holder = playerConnectEvent.getHolder();
        var questStoreComponent = holder.ensureAndGetComponent(QuestStoreComponent.getComponentType());
        questStoreComponent.loadQuests();

        // Universe-scope quests are assigned to every player on connection.
        QuestService.get().addQuestsToPlayerStore(QuestService.get().universeStore.getAllIds(), holder);
    }
}
