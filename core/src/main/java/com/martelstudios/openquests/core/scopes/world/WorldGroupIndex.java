package com.martelstudios.openquests.core.scopes.world;

import com.hypixel.hytale.server.core.universe.world.World;
import com.martelstudios.openquests.core.OpenQuestsCorePlugin;
import com.martelstudios.openquests.core.persistence.QuestStorage;
import com.martelstudios.openquests.core.stores.QuestsRecord;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The groups of worlds sharing quests, each named after the assignment gathering it. Unlike a
 * world's, a group's index lives on no world: it is kept here, read back the first time, and
 * written out with the rest. A group never closes; its worlds come and go.
 */
public class WorldGroupIndex {

    /**
     * What a group's index is written under, next to the worlds' own.
     */
    public static final String GROUP_INDEX_PREFIX = "worlds:";

    @Nonnull
    private final QuestStorage storage;

    private final Map<String, QuestsRecord> groups = new ConcurrentHashMap<>();

    private final Set<String> dirtyGroups = ConcurrentHashMap.newKeySet();

    /**
     * The worlds of each group someone entered since they opened, in memory only: the ones a quest
     * the group starts now has players to reach in. The others take it up as they are entered.
     */
    private final Map<String, Set<UUID>> joinedWorlds = new ConcurrentHashMap<>();

    public WorldGroupIndex(@Nonnull QuestStorage storage) {
        this.storage = storage;
    }

    public static WorldGroupIndex get() {
        return OpenQuestsCorePlugin.get().getWorldGroupIndex();
    }

    /**
     * @return the key a group's index and records are written under.
     */
    @Nonnull
    public static String keyOf(@Nonnull String group) {
        return GROUP_INDEX_PREFIX + group;
    }

    /**
     * @return the ids on that group's index, those that ended included, read back the first time.
     */
    @Nonnull
    public Set<UUID> getQuestIds(@Nonnull String group) {
        return record(group).getAllIds();
    }

    /**
     * Puts a quest in a group: the worlds it gathers take it up as they are entered.
     */
    public void add(@Nonnull String group, @Nonnull UUID questId) {
        if (record(group).register(questId)) dirtyGroups.add(group);
    }

    /**
     * Takes a quest out of a group, for one leaving for good.
     */
    public void remove(@Nonnull String group, @Nonnull UUID questId) {
        QuestsRecord record = groups.get(group);
        if (record != null && record.unregister(questId)) dirtyGroups.add(group);
    }

    /**
     * Counts a world of the group as entered, so that what the group starts next reaches it.
     */
    public void join(@Nonnull String group, @Nonnull World world) {
        joinedWorlds.computeIfAbsent(group, key -> ConcurrentHashMap.newKeySet()).add(world.getWorldConfig().getUuid());
    }

    /**
     * @return the worlds of the group entered since they opened and still open.
     */
    @Nonnull
    public List<World> getJoinedWorlds(@Nonnull String group) {
        Set<UUID> worldIds = joinedWorlds.get(group);
        return worldIds == null ? List.of() : WorldQuestService.openWorlds(worldIds);
    }

    /**
     * Forgets a world closing for good, in every group it was entered for.
     */
    public void forgetWorld(@Nonnull UUID worldId) {
        for (Set<UUID> worldIds : joinedWorlds.values()) {
            worldIds.remove(worldId);
        }
    }

    /**
     * Writes out the index of every group that changed, for the save pass and for shutdown.
     */
    public void saveAll(boolean force) {
        for (String group : force ? Set.copyOf(groups.keySet()) : Set.copyOf(dirtyGroups)) {
            dirtyGroups.remove(group);

            QuestsRecord record = groups.get(group);
            if (record != null) storage.saveIndex(keyOf(group), record.getAllIds());
        }
    }

    @Nonnull
    private QuestsRecord record(@Nonnull String group) {
        QuestsRecord record = groups.get(group);
        if (record != null) return record;

        // Read outside the map's lock, which a slow storage would otherwise hold for every group
        QuestsRecord read = new QuestsRecord(storage.loadIndex(keyOf(group)));
        QuestsRecord raced = groups.putIfAbsent(group, read);
        return raced != null ? raced : read;
    }
}
