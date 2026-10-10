package com.martelstudios.openquests.core.scopes.world;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.RemovedPlayerFromWorldEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.events.StartWorldEvent;
import com.martelstudios.openquests.core.OpenQuestsCorePlugin;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.QuestOperation;
import com.martelstudios.openquests.core.models.QuestScope;
import com.martelstudios.openquests.core.scopes.ScopeIndexes;
import com.martelstudios.openquests.core.scopes.player.PlayerQuestService;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.core.stores.QuestStoreComponent;
import com.martelstudios.openquests.core.sync.QuestSyncService;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds the quests shared by every player of a world: assigns them on world entry, takes them back
 * on world exit, and has each quest's scope note the worlds holding it, which is how a quest finds
 * its worlds and a world its quests. Whatever touches a world's players does so on its thread.
 */
public class WorldQuestService {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /**
     * What a world's index is written under. One namespace for the lot, so a scope added later
     * picks a prefix of its own.
     */
    public static final String WORLD_INDEX_PREFIX = "world:";

    @Nonnull
    private final ScopeIndexes indexes;

    /**
     * The worlds closing for good: nothing is shared into them any more, so a quest paying with
     * another on its way out leaves nothing behind in a world that is gone.
     */
    private final Set<UUID> closing = ConcurrentHashMap.newKeySet();

    public WorldQuestService(@Nonnull JavaPlugin plugin, @Nonnull ScopeIndexes indexes) {
        this.indexes = indexes;

        plugin.getEventRegistry().registerGlobal(AddPlayerToWorldEvent.class, this::handleAddPlayerToWorldEvent);
        plugin.getEventRegistry().registerGlobal(RemovedPlayerFromWorldEvent.class, this::handleRemovedPlayerFromWorldEvent);
        plugin.getEventRegistry().registerGlobal(StartWorldEvent.class, event -> preload(event.getWorld()));
    }

    public static WorldQuestService get() {
        return OpenQuestsCorePlugin.get().getWorldQuestService();
    }

    @Nonnull
    public static String indexKey(@Nonnull World world) {
        return WORLD_INDEX_PREFIX + world.getWorldConfig().getUuid();
    }

    /**
     * Shares the quest with everyone inside, now and as they come in, on the world's own thread
     * whoever calls. A quest several worlds hold is one progression they all push.
     */
    public void addQuest(@Nonnull World world, @Nonnull UUID questId) {
        onThreadOf(world, () -> addQuestHere(world, questId));
    }

    /**
     * Takes the quest off the world's index, in order with what was handed to the world before.
     */
    public void removeQuest(@Nonnull World world, @Nonnull UUID questId) {
        onThreadOf(world, () -> removeQuestHere(world, questId));
    }

    /**
     * Takes an ended quest off the world's index of running ones, in order with what was handed
     * to the world before. The quest keeps the world in its scope, and its players keep it in
     * their journals.
     */
    public void unindex(@Nonnull World world, @Nonnull UUID questId) {
        onThreadOf(world, () -> indexes.remove(indexKey(world), questId));
    }

    /**
     * Reads a world's index and the quests it runs back off the game threads as the world starts,
     * so the first player in finds them in memory. One entering first reads them on the spot.
     */
    public void preload(@Nonnull World world) {
        QuestSyncService.get().execute("the quests of world " + world.getName(), () -> QuestProgressionService.get().loadQuests(getQuestIds(world)));
    }

    /**
     * @return the ids on that world's index of running quests, read back first if nothing did yet.
     */
    @Nonnull
    public Set<UUID> getQuestIds(@Nonnull World world) {
        return indexes.getIds(indexKey(world));
    }

    /**
     * @return those of the worlds that are open, the only ones a quest can be shared in. A world
     * being closed is left out, so that nothing is shared into it.
     */
    @Nonnull
    public List<World> openWorlds(@Nonnull Collection<UUID> worldIds) {
        List<World> worlds = new ArrayList<>();
        for (UUID worldId : worldIds) {
            World world = Universe.get().getWorld(worldId);
            if (world != null && !closing.contains(worldId)) worlds.add(world);
        }
        return worlds;
    }

    /**
     * Written by the closing of the world alone, and cleared again once it is closed or called off.
     */
    void setClosing(@Nonnull World world, boolean isClosing) {
        UUID worldId = world.getWorldConfig().getUuid();
        if (isClosing) {
            closing.add(worldId);
        } else {
            closing.remove(worldId);
        }
    }

    /**
     * Runs the work on the world's own thread, the only one allowed to touch it: at once if
     * already there, later otherwise.
     */
    public static void onThreadOf(@Nonnull World world, @Nonnull Runnable work) {
        if (world.isInThread()) {
            work.run();
        } else {
            world.execute(work);
        }
    }

    /**
     * On whatever thread the caller is on: a world closing may have no thread left to run on.
     */
    void removeQuestHere(@Nonnull World world, @Nonnull UUID questId) {
        LOGGER.atInfo().log("Removing quest %s from world %s", questId, world.getName());

        indexes.remove(indexKey(world), questId);

        // Gone already when it left for good, and nothing then is left to write on
        AbstractQuestProgression<?> quest = QuestProgressionService.get().getQuest(questId);
        UUID worldId = world.getWorldConfig().getUuid();
        if (quest != null) quest.apply(new QuestOperation.RemoveWorld(worldId));
    }

    /**
     * Lets go of a world's index for good, in memory and in the storage.
     */
    void deleteIndex(@Nonnull World world) {
        indexes.delete(indexKey(world));
    }

    private void addQuestHere(@Nonnull World world, @Nonnull UUID questId) {
        AbstractQuestProgression<?> quest = QuestProgressionService.get().loadQuest(questId);
        if (quest == null) return;

        if (!indexes.add(indexKey(world), questId)) return;

        LOGGER.atInfo().log("Added quest %s to world %s", questId, world.getName());

        UUID worldId = world.getWorldConfig().getUuid();
        QuestScope scope = quest.getScope();
        if (scope == null) {
            quest.setScope(new WorldQuestScope(List.of(worldId)));
        } else {
            quest.apply(new QuestOperation.AddWorld(worldId));
        }

        for (PlayerRef playerRef : world.getPlayerRefs()) {
            QuestProgressionService.get().joinQuest(quest, playerRef.getUuid());
        }
    }

    /**
     * Assigns this world's quests to the entering player, reading the index back on the first
     * one in.
     */
    private void handleAddPlayerToWorldEvent(@Nonnull AddPlayerToWorldEvent addPlayerToWorldEvent) {
        var playerRef = addPlayerToWorldEvent.getHolder().getComponent(PlayerRef.getComponentType());
        if (playerRef == null) return;

        for (AbstractQuestProgression<?> quest : indexes.resolve(indexKey(addPlayerToWorldEvent.getWorld()))) {
            QuestProgressionService.get().joinQuest(quest, playerRef.getUuid());
        }
    }

    /**
     * Takes this world's running quests back from the leaving player. What ended while they were
     * here stays theirs, in their journal, the way any finished quest does.
     *
     * <p>Their index is updated through the holder they leave with: their entity is on its way out of
     * this world, and work queued for it here would find nothing left to write on.
     */
    private void handleRemovedPlayerFromWorldEvent(@Nonnull RemovedPlayerFromWorldEvent removedPlayerFromWorldEvent) {
        var playerRef = removedPlayerFromWorldEvent.getHolder().getComponent(PlayerRef.getComponentType());
        if (playerRef == null) return;

        QuestStoreComponent playerStore = removedPlayerFromWorldEvent.getHolder().getComponent(QuestStoreComponent.getComponentType());

        for (AbstractQuestProgression<?> quest : indexes.resolve(indexKey(removedPlayerFromWorldEvent.getWorld()))) {
            if (QuestProgressionService.get().getLiveQuest(quest.getId()) == null) continue;

            if (quest.removePlayer(playerRef.getUuid()) && playerStore != null) {
                PlayerQuestService.get().removeQuestFromPlayerStore(playerStore, quest, playerRef.getUuid());
            }
        }
    }
}
