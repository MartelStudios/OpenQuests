package com.martelstudios.hyquests.extension.quests.gather;

import com.martelstudios.hyquests.core.events.QuestPlayerAddedEvent;
import com.martelstudios.hyquests.core.utils.EntityComponents;

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

        EntityComponents.update(event.getPlayerId(), components -> gatherQuest.update(new GatherQuestVisitor(components)));
    }

}
