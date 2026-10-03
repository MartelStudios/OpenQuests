package com.martelstudios.openquests.core.scopes.world;

import com.hypixel.hytale.event.EventPriority;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.RemovedPlayerFromWorldEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.events.RemoveWorldEvent;
import com.martelstudios.openquests.core.OpenQuestsCorePlugin;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.persistence.QuestStorage;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.core.stores.QuestsRecord;
import com.martelstudios.openquests.core.visitors.SetStateVisitor;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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

    /**
     * Worlds being closed, which nothing is shared into any more: a quest failed on the way out
     * and paying with another quest would otherwise leave it behind in a world that is gone.
     */
    private static final Set<UUID> CLOSING = ConcurrentHashMap.newKeySet();

    private final QuestStorage storage;

    public WorldQuestService(@Nonnull JavaPlugin plugin, @Nonnull QuestStorage storage) {
        this.storage = storage;

        plugin.getEventRegistry().registerGlobal(AddPlayerToWorldEvent.class, this::handleAddPlayerToWorldEvent);
        plugin.getEventRegistry().registerGlobal(RemovedPlayerFromWorldEvent.class, this::handleRemovedPlayerFromWorldEvent);

        // Last, so a removal another plugin called off is seen as such
        plugin.getEventRegistry().registerGlobal(EventPriority.LAST, RemoveWorldEvent.class, this::handleRemoveWorldEvent);
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
     * @return those of the worlds that are open, the only ones a quest can be shared in. A world
     * being closed is left out, so that nothing is shared into it.
     */
    @Nonnull
    public static List<World> openWorlds(@Nonnull Collection<UUID> worldIds) {
        List<World> worlds = new ArrayList<>();
        for (UUID worldId : worldIds) {
            World world = Universe.get().getWorld(worldId);
            if (world != null && !CLOSING.contains(worldId)) worlds.add(world);
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
     * Fails what a world still runs as it closes for good, an instance done with or a world removed
     * by hand, then lets go of its index. A crash keeps everything, the world being reloaded, and
     * so does a server stop, which raises no removal at all.
     */
    private void handleRemoveWorldEvent(@Nonnull RemoveWorldEvent removeWorldEvent) {
        if (removeWorldEvent.isCancelled()) return;
        if (removeWorldEvent.getRemovalReason() != RemoveWorldEvent.RemovalReason.GENERAL) return;

        World world = removeWorldEvent.getWorld();
        UUID worldId = world.getWorldConfig().getUuid();
        QuestsRecord questsRecord = loadedRecord(world);

        CLOSING.add(worldId);
        try {
            for (UUID questId : new ArrayList<>(questsRecord.getAllIds())) {
                AbstractQuestProgression<?> quest = QuestProgressionService.get().loadQuest(questId);
                if (quest != null) close(world, quest);
            }

            questsRecord.replaceAll(Set.of());
            getWorldQuestStoreFromWorld(world).consumeChanges();
            storage.deleteIndex(indexKey(world));

            LOGGER.atInfo().log("Closed the quests of world %s", world.getName());
        } finally {
            CLOSING.remove(worldId);
        }
    }

    /**
     * A quest another open world shares goes on there. Otherwise one still running fails, and one
     * no journal holds is done away with, nothing being left to ever read it; the rest stays with
     * whoever holds it, in memory only while one of them is online.
     */
    private void close(@Nonnull World world, @Nonnull AbstractQuestProgression<?> quest) {
        UUID questId = quest.getId();

        if (quest.getScope() instanceof WorldQuestScope scope && !openWorlds(scope.getWorlds()).isEmpty()) {
            removeQuest(world, questId);
            return;
        }

        if (QuestProgressionService.get().getLiveQuest(questId) != null) {
            QuestProgressionService.get().progress(new SetStateVisitor(QuestState.FAILED), List.of(questId));
        }

        if (quest.getPlayers().isEmpty() && quest.getAbandonedPlayers().isEmpty()) {
            QuestProgressionService.get().unregisterQuest(quest);
        } else if (!isHeldOnline(quest)) {
            QuestProgressionService.get().unloadQuest(questId);
        }
    }

    private static boolean isHeldOnline(@Nonnull AbstractQuestProgression<?> quest) {
        for (UUID playerId : quest.getPlayers()) {
            if (Universe.get().getPlayer(playerId) != null) return true;
        }

        for (UUID playerId : quest.getAbandonedPlayers()) {
            if (Universe.get().getPlayer(playerId) != null) return true;
        }

        return false;
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
