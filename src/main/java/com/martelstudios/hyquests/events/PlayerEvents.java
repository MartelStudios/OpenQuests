package com.martelstudios.hyquests.events;

import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.martelstudios.hyquests.services.PlayerQuestService;
import com.martelstudios.hyquests.services.UniverseQuestService;
import com.martelstudios.hyquests.stores.QuestStoreComponent;

public class PlayerEvents {
    public static void handlePlayerConnectEvent(PlayerConnectEvent playerConnectEvent) {
        var holder = playerConnectEvent.getHolder();
        var questStoreComponent = holder.ensureAndGetComponent(QuestStoreComponent.getComponentType());
        questStoreComponent.loadQuests();

        // Universe-scope quests are assigned to every player on connection.
        PlayerQuestService.get().addQuestsToPlayerStore(UniverseQuestService.get().universeQuests.getAllIds(), holder);
    }
}
