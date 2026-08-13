package com.martelstudios.hyquests.events;

import com.hypixel.hytale.server.core.universe.Universe;
import com.martelstudios.hyquests.models.GatherQuest;
import com.martelstudios.hyquests.visitors.GatherQuestVisitor;

import java.util.UUID;

public class QuestEvents {
    /**
     * Catches a freshly assigned {@link GatherQuest} up with what the player already holds, so
     * they do not have to touch their inventory again for progress to show.
     */
    public static void handleQuestAssignedToPlayer(QuestPlayerAddedEvent event) {
        if (!(event.getQuest() instanceof GatherQuest gatherQuest)) return;

        UUID playerId = event.getPlayerId();

        var playerRef = Universe.get().getPlayer(playerId);

        if (playerRef != null && playerRef.getReference() != null) {
            gatherQuest.update(new GatherQuestVisitor(playerRef.getReference()));
        } else {
            Universe.get()
                    .getPlayerStorage()
                    .update(playerId, holder -> gatherQuest.update(new GatherQuestVisitor(holder)));
        }
    }

    public static void handleQuestRegistered(QuestRegisteredEvent questRegisteredEvent) {
        questRegisteredEvent.getQuest().onRegistered();
    }

    public static void handleQuestUnregistered(QuestUnregisteredEvent questUnregisteredEvent) {
        questUnregisteredEvent.getQuest().onUnregistered();
    }
}
