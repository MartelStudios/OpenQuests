package com.martelstudios.hyquests.extension.gather;

import com.hypixel.hytale.server.core.universe.Universe;
import com.martelstudios.hyquests.core.events.QuestPlayerAddedEvent;

import java.util.UUID;

/**
 * Handlers tied to a concrete quest type rather than to a service, kept out of the core services
 * so those stay agnostic of the quest types shipped on top of them.
 */
public class GatherEvents {
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

}
