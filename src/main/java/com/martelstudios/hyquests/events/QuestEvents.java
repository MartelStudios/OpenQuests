package com.martelstudios.hyquests.events;

import com.hypixel.hytale.server.core.universe.Universe;
import com.martelstudios.hyquests.PlayerAccess;
import com.martelstudios.hyquests.models.GatherQuest;
import com.martelstudios.hyquests.services.QuestProgressionService;
import com.martelstudios.hyquests.visitors.GatherQuestVisitor;

import java.util.UUID;

public class QuestEvents {
    /**
     * Catches a freshly assigned {@link GatherQuest} up with what the player already holds, so
     * they do not have to touch their inventory again for progress to show.
     */
    public static void handleQuestAssignedToPlayer(QuestAssignedToPlayerEvent event) {
        if (!(event.getQuest() instanceof GatherQuest gatherQuest)) return;

        UUID playerId = event.getPlayerId();

        // Stored data is stale for an online player, so only fall back to it when they are away
        boolean online = QuestProgressionService.get()
                                                .withOnlinePlayer(playerId, access -> gatherQuest.update(new GatherQuestVisitor(playerId, access)));
        if (online) return;

        Universe.get()
                .getPlayerStorage()
                .update(playerId, holder -> gatherQuest.update(new GatherQuestVisitor(playerId, PlayerAccess.of(holder))));
    }

    public static void handleQuestRegistered(QuestRegisteredEvent questRegisteredEvent) {
        questRegisteredEvent.getQuest().onRegistered();
    }

    public static void handleQuestUnregistered(QuestUnregisteredEvent questUnregisteredEvent) {
        questUnregisteredEvent.getQuest().onUnregistered();
    }
}
