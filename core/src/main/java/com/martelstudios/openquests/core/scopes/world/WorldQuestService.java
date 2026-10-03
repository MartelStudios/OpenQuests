package com.martelstudios.openquests.core.scopes.world;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.RemovedPlayerFromWorldEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.martelstudios.openquests.core.OpenQuestsCorePlugin;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.persistence.QuestStorage;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.core.stores.QuestsRecord;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Holds the quests shared by every player of a world: assigns them on world entry, takes them back
 * on world exit, and writes on each quest the worlds holding it ({@link WorldQuestScope}), which
 * is how a quest finds its worlds and a world its quests.
 */
public class WorldQuestService {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /**
     * What a world's index is written under. One namespace for the lot, so a scope added later
     * picks a prefix of its own.
     */
    public static final String WORLD_INDEX_PREFIX = "world:";

    private final QuestStorage storage;

    public WorldQuestService(@Nonnull JavaPlugin plugin, @Nonnull QuestStorage storage) {
        this.storage = storage;

        plugin.getEventRegistry().registerGlobal(AddPlayerToWorldEvent.class, this::handleAddPlayerToWorldEvent);
        plugin.getEventRegistry().registerGlobal(RemovedPlayerFromWorldEvent.class, this::handleRemovedPlayerFromWorldEvent);
    }

    public static WorldQuestService get() {
        return OpenQuestsCorePlugin.get().getWorldQuestService();
    }

    public static WorldQuestStoreResource getWorldQuestStoreFromWorld(@Nonnull World world) {
        return world.getEntityStore().getStore().getResource(WorldQuestStoreResource.getResourceType());
    }

    @Nonnull
    public static String indexKey(@Nonnull World world) {
        return WORLD_INDEX_PREFIX + world.getWorldConfig().getUuid();
    }

    /**
     * Shares the quest with everyone inside, now and as they come in. A quest several worlds hold
     * is one progression they all push.
     */
    public void addQuest(@Nonnull World world, @Nonnull UUID questId) {
        AbstractQuestProgression<?> quest = QuestProgressionService.get().loadQuest(questId);
        if (quest == null) return;

        if (!loadedRecord(world).register(questId)) return;
        getWorldQuestStoreFromWorld(world).markDirty();

        LOGGER.atInfo().log("Added quest %s to world %s", questId, world.getName());

        UUID worldId = world.getWorldConfig().getUuid();
        if (quest.getScope() instanceof WorldQuestScope scope) {
            if (scope.getWorlds().add(worldId)) quest.markDirty();
        } else {
            quest.setScope(new WorldQuestScope(worldId));
        }

        for (PlayerRef playerRef : world.getPlayerRefs()) {
            QuestProgressionService.get().joinQuest(quest, playerRef.getUuid());
        }
    }

    public void removeQuest(@Nonnull World world, @Nonnull UUID questId) {
        LOGGER.atInfo().log("Removing quest %s from world %s", questId, world.getName());

        WorldQuestStoreResource store = getWorldQuestStoreFromWorld(world);
        if (store.questsRecord.unregister(questId)) store.markDirty();

        // Gone already when it left for good, and nothing then is left to write on
        AbstractQuestProgression<?> quest = QuestProgressionService.get().getQuest(questId);
        if (quest != null && quest.getScope() instanceof WorldQuestScope scope && scope.getWorlds().remove(world.getWorldConfig().getUuid())) {
            quest.markDirty();
        }
    }

    /**
     * @return the ids on that world's index, those that ended included, read back first if no
     * player has come in yet.
     */
    @Nonnull
    public Set<UUID> getQuestIds(@Nonnull World world) {
        return loadedRecord(world).getAllIds();
    }

    /**
     * @return those of the worlds that are open, the only ones a quest can be shared in.
     */
    @Nonnull
    public static List<World> openWorlds(@Nonnull Collection<UUID> worldIds) {
        List<World> worlds = new ArrayList<>();
        for (UUID worldId : worldIds) {
            World world = Universe.get().getWorld(worldId);
            if (world != null) worlds.add(world);
        }
        return worlds;
    }

    /**
     * Writes out the index of every world that changed, for the save pass and for shutdown.
     */
    public void saveAll(boolean force) {
        for (World world : Universe.get().getWorlds().values()) {
            WorldQuestStoreResource store = getWorldQuestStoreFromWorld(world);
            if (store == null) continue;

            if (!store.consumeChanges() && !force) continue;

            storage.saveIndex(indexKey(world), store.questsRecord.getAllIds());
        }
    }

    /**
     * Assigns this world's quests to the entering player, reading the index back on the first
     * one in.
     */
    private void handleAddPlayerToWorldEvent(@Nonnull AddPlayerToWorldEvent addPlayerToWorldEvent) {
        var playerRef = addPlayerToWorldEvent.getHolder().getComponent(PlayerRef.getComponentType());
        if (playerRef == null) return;

        var world = addPlayerToWorldEvent.getWorld();
        WorldQuestStoreResource store = getWorldQuestStoreFromWorld(world);
        QuestsRecord questsRecord = loadedRecord(world);

        for (UUID questId : new ArrayList<>(questsRecord.getAllIds())) {
            var quest = QuestProgressionService.get().loadQuest(questId);
            if (quest == null) {
                // Left on the index, so that a quest set aside comes back with its asset
                if (QuestProgressionService.get().isSetAside(questId)) continue;

                questsRecord.unregister(questId);
                store.markDirty();
                continue;
            }

            QuestProgressionService.get().joinQuest(quest, playerRef.getUuid());
        }
    }

    /**
     * Takes this world's running quests back from the leaving player. What ended while they were
     * here stays theirs, in their journal, the way any finished quest does.
     */
    private void handleRemovedPlayerFromWorldEvent(@Nonnull RemovedPlayerFromWorldEvent removedPlayerFromWorldEvent) {
        var playerRef = removedPlayerFromWorldEvent.getHolder().getComponent(PlayerRef.getComponentType());
        if (playerRef == null) return;

        WorldQuestStoreResource store = getWorldQuestStoreFromWorld(removedPlayerFromWorldEvent.getWorld());
        QuestsRecord questsRecord = store.questsRecord;

        for (UUID questId : new ArrayList<>(questsRecord.getAllIds())) {
            var quest = QuestProgressionService.get().loadQuest(questId);
            if (quest == null) {
                // Left on the index, so that a quest set aside comes back with its asset
                if (QuestProgressionService.get().isSetAside(questId)) continue;

                questsRecord.unregister(questId);
                store.markDirty();
                continue;
            }

            if (QuestProgressionService.get().getLiveQuest(questId) == null) continue;

            quest.removePlayer(playerRef.getUuid());
        }
    }

    /**
     * The index of a world, read back the first time anything asks for it, whoever asks first:
     * the first player in, or something handing the world a quest before anyone came.
     */
    @Nonnull
    private QuestsRecord loadedRecord(@Nonnull World world) {
        WorldQuestStoreResource store = getWorldQuestStoreFromWorld(world);

        if (store.consumeNeedsLoad()) {
            store.questsRecord.replaceAll(storage.loadIndex(indexKey(world)));
        }
        return store.questsRecord;
    }
}
