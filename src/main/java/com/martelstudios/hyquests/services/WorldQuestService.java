package com.martelstudios.hyquests.services;

import com.hypixel.hytale.event.EventRegistration;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.RemovedPlayerFromWorldEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.martelstudios.hyquests.HyQuestsPlugin;
import com.martelstudios.hyquests.events.QuestUnregisteredEvent;
import com.martelstudios.hyquests.models.AbstractQuest;
import com.martelstudios.hyquests.stores.QuestsRecord;
import com.martelstudios.hyquests.stores.WorldQuestStoreResource;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds the quests shared by every player of a world. Quests know nothing about this scope: the
 * service assigns them on world entry and takes them back on world exit.
 */
public class WorldQuestService {
    @Nonnull
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final ConcurrentHashMap<UUID, EventRegistration<UUID, QuestUnregisteredEvent>> questUnregisteredListeners = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<UUID, Set<UUID>> questsToWorlds = new ConcurrentHashMap<>();

    public static WorldQuestService get() {
        return HyQuestsPlugin.get().getWorldQuestService();
    }

    public static WorldQuestStoreResource getWorldQuestStoreFromWorld(@Nonnull World world) {
        return world.getEntityStore().getStore().getResource(WorldQuestStoreResource.getResourceType());
    }

    public void addQuest(@Nonnull World world, @Nonnull UUID questId) {
        AbstractQuest<?> quest = QuestProgressionService.get().getQuest(questId);
        if (quest == null) return;

        if (!getWorldQuestStoreFromWorld(world).questsRecord.register(questId)) return;

        LOGGER.atInfo().log("Added quest %s to world %s", questId, world.getName());

        trackQuestForWorld(world, questId);
        quest.addPlayersFromPlayerRef(world.getPlayerRefs());
    }

    public void removeQuest(@Nonnull World world, @Nonnull UUID questId) {
        LOGGER.atInfo().log("Removing quest %s from world %s", questId, world.getName());

        getWorldQuestStoreFromWorld(world).questsRecord.unregister(questId);
        untrackQuestForWorld(world, questId);
    }

    /**
     * Assigns this world's quests to the entering player.
     */
    public void handleAddPlayerToWorldEvent(@Nonnull AddPlayerToWorldEvent addPlayerToWorldEvent) {
        var playerRef = addPlayerToWorldEvent.getHolder().getComponent(PlayerRef.getComponentType());
        if (playerRef == null) return;

        var world = addPlayerToWorldEvent.getWorld();
        QuestsRecord questsRecord = getWorldQuestStoreFromWorld(world).questsRecord;
        questsRecord.loadAll();

        for (UUID questId : new ArrayList<>(questsRecord.getAllIds())) {
            var quest = QuestProgressionService.get().getQuest(questId);
            if (quest == null) {
                questsRecord.unregister(questId);
                continue;
            }

            trackQuestForWorld(world, questId);
            quest.addPlayer(playerRef.getUuid());
        }
    }

    /**
     * Takes this world's quests back from the leaving player.
     */
    public void handleRemovedPlayerFromWorldEvent(@Nonnull RemovedPlayerFromWorldEvent removedPlayerFromWorldEvent) {
        var playerRef = removedPlayerFromWorldEvent.getHolder().getComponent(PlayerRef.getComponentType());
        if (playerRef == null) return;

        QuestsRecord questsRecord = getWorldQuestStoreFromWorld(removedPlayerFromWorldEvent.getWorld()).questsRecord;

        for (UUID questId : new ArrayList<>(questsRecord.getAllIds())) {
            var quest = QuestProgressionService.get().getQuest(questId);
            if (quest == null) {
                questsRecord.unregister(questId);
                continue;
            }

            quest.removePlayer(playerRef.getUuid());
        }
    }

    private void handleQuestUnregisteredEvent(QuestUnregisteredEvent questUnregisteredEvent) {
        UUID questId = questUnregisteredEvent.getQuest().getId();
        Set<UUID> worlds = questsToWorlds.get(questId);

        if (worlds != null) {
            // Snapshot: removeQuest mutates this very set
            for (UUID worldId : Set.copyOf(worlds)) {
                World world = Universe.get().getWorld(worldId);
                if (world != null) removeQuest(world, questId);
            }
        }

        // Drop what is left, e.g. worlds that no longer resolve
        questsToWorlds.remove(questId);
        releaseListener(questId);
    }

    /**
     * One listener per quest, however many worlds hold it.
     */
    private void trackQuestForWorld(World world, UUID questId) {
        questsToWorlds.computeIfAbsent(questId, key -> ConcurrentHashMap.newKeySet())
                      .add(world.getWorldConfig().getUuid());

        questUnregisteredListeners.computeIfAbsent(questId, id -> HytaleServer.get()
                                                                             .getEventBus()
                                                                             .register(QuestUnregisteredEvent.class, id, this::handleQuestUnregisteredEvent));
    }

    private void untrackQuestForWorld(World world, UUID questId) {
        var worlds = questsToWorlds.computeIfPresent(questId, (id, knownWorlds) -> {
            knownWorlds.remove(world.getWorldConfig().getUuid());
            return knownWorlds.isEmpty() ? null : knownWorlds;
        });

        if (worlds == null) releaseListener(questId);
    }

    private void releaseListener(UUID questId) {
        var questListener = questUnregisteredListeners.remove(questId);
        if (questListener != null) {
            questListener.unregister();
        }
    }
}
