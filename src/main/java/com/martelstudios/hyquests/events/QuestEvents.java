package com.martelstudios.hyquests.events;

import com.hypixel.hytale.server.core.universe.Universe;
import com.martelstudios.hyquests.models.GatherQuest;
import com.martelstudios.hyquests.visitors.GatherQuestVisitor;

import java.util.UUID;

public class QuestEvents {
    public static void handleQuestAssignedToPlayer(QuestAssignedToPlayerEvent event) {
        if (!(event.getQuest() instanceof GatherQuest gatherQuest)) return;

        UUID playerId = event.getPlayerId();
        Universe.get()
                .getPlayerStorage()
                .update(playerId, holder -> gatherQuest.update(new GatherQuestVisitor(playerId, holder)));
    }

    public static void handleQuestRegistered(QuestRegisteredEvent questRegisteredEvent) {
        questRegisteredEvent.getQuest().onRegistered();
    }

    public static void handleQuestUnregistered(QuestUnregisteredEvent questUnregisteredEvent) {
        questUnregisteredEvent.getQuest().onUnregistered();
    }
}
