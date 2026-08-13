package com.martelstudios.hyquests.events;

import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.martelstudios.hyquests.services.QuestHistoryService;

public class WorldEvents {
    /**
     * Assigns this world's quests to the entering player, and hands over what completions that
     * happened while they were away could not grant. Done here rather than on connection so the
     * player is actually in a world, ready to be shown the reward.
     */
    public static void handleAddPlayerToWorldEvent(AddPlayerToWorldEvent addPlayerToWorldEvent) {
        var holder = addPlayerToWorldEvent.getHolder();
        var playerRef = holder.getComponent(PlayerRef.getComponentType());
        if (playerRef == null || playerRef.getReference() == null) return;

        QuestHistoryService.get().claimAutoRewards(playerRef.getReference());
    }
}
