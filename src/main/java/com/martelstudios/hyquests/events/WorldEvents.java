package com.martelstudios.hyquests.events;

import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.martelstudios.hyquests.PlayerAccess;
import com.martelstudios.hyquests.services.QuestProgressionService;

public class WorldEvents {
    /**
     * Assigns this world's quests to the entering player, and hands over what completions that
     * happened while they were away could not grant. Done here rather than on connection so the
     * player is actually in a world, ready to be shown the reward.
     */
    public static void handleAddPlayerToWorldEvent(AddPlayerToWorldEvent addPlayerToWorldEvent) {
        var holder = addPlayerToWorldEvent.getHolder();

        // Safe to re-run on every world change: a granted record is no longer claimable
        var playerId = holder.getComponent(PlayerRef.getComponentType()).getUuid();
        QuestProgressionService.get().claimAutoRewards(playerId, PlayerAccess.of(holder));
    }
}
