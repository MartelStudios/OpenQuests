package com.martelstudios.hyquests.events;

import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.RemovedPlayerFromWorldEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.martelstudios.hyquests.PlayerAccess;
import com.martelstudios.hyquests.services.QuestService;
import com.martelstudios.hyquests.stores.QuestStoreComponent;
import com.martelstudios.hyquests.stores.WorldQuestStoreResource;

import java.util.UUID;

public class WorldEvents {
    /**
     * Assigns this world's quests to the entering player, and hands over what completions that
     * happened while they were away could not grant. Done here rather than on connection so the
     * player is actually in a world, ready to be shown the reward.
     */
    public static void handleAddPlayerToWorldEvent(AddPlayerToWorldEvent addPlayerToWorldEvent) {
        var holder = addPlayerToWorldEvent.getHolder();
        var store = addPlayerToWorldEvent.getWorld().getEntityStore().getStore();
        var worldQuestStore = store.getResource(WorldQuestStoreResource.getResourceType());
        worldQuestStore.loadQuests();

        QuestService.get().addQuestsToPlayerStore(worldQuestStore.questStore.getAllIds(), holder);

        // Safe to re-run on every world change: a granted record is no longer claimable
        var playerId = holder.getComponent(PlayerRef.getComponentType()).getUuid();
        QuestService.get().claimAutoRewards(playerId, PlayerAccess.of(holder));
    }

    /**
     * Removes this world's quests from the leaving player, except if the quest also has the player registered directly
     */
    public static void handleRemovedPlayerFromWorldEvent(RemovedPlayerFromWorldEvent removedPlayerFromWorldEvent) {
        var holder = removedPlayerFromWorldEvent.getHolder();
        var playerQuestStore = holder.getComponent(QuestStoreComponent.getComponentType());
        if (playerQuestStore == null) return;

        var playerRef = holder.getComponent(PlayerRef.getComponentType());

        var store = removedPlayerFromWorldEvent.getWorld().getEntityStore().getStore();
        var worldQuestStore = store.getResource(WorldQuestStoreResource.getResourceType());

        for (UUID questId : worldQuestStore.questStore.getAllIds()) {
            var quest = QuestService.get().getQuest(questId);

            if (quest == null) {
                worldQuestStore.questStore.unregister(questId);
                continue;
            }

            // Do not unregister quest from player store if the player is directly registered in the quest
            if (quest.getPlayers().contains(playerRef.getUuid())) continue;

            playerQuestStore.questStore.unregister(questId);
        }
    }
}
