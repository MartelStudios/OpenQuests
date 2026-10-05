package com.martelstudios.openquests.core.scopes.world;

import com.hypixel.hytale.server.core.universe.world.World;
import com.martelstudios.openquests.core.OpenQuestsCorePlugin;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.scopes.ScopeIndexes;
import com.martelstudios.openquests.core.services.QuestProgressionService;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The groups of worlds sharing quests, each named after the assignment gathering it. Unlike a
 * world's, a group's quests live on no world. A group never closes; its worlds come and go.
 */
public class WorldGroupIndex {

    /**
     * What a group's index is written under, next to the worlds' own.
     */
    public static final String GROUP_INDEX_PREFIX = "worlds:";

    @Nonnull
    private final ScopeIndexes indexes;

    /**
     * The worlds of each group someone entered since they opened, in memory only: the ones a quest
     * the group starts now has players to reach in. The others take it up as they are entered.
     */
    private final Map<String, Set<UUID>> joinedWorlds = new ConcurrentHashMap<>();

    public WorldGroupIndex(@Nonnull ScopeIndexes indexes) {
        this.indexes = indexes;
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
     * @return the ids of the group's running quests, read back the first time.
     */
    @Nonnull
    public Set<UUID> getQuestIds(@Nonnull String group) {
        return indexes.getIds(keyOf(group));
    }

    /**
     * Puts a quest in a group: the worlds it gathers take it up as they are entered.
     */
    public void add(@Nonnull String group, @Nonnull UUID questId) {
        indexes.add(keyOf(group), questId);
    }

    /**
     * Takes a quest out of a group, for one ending or leaving for good.
     */
    public void remove(@Nonnull String group, @Nonnull UUID questId) {
        indexes.remove(keyOf(group), questId);
    }

    /**
     * Counts a world of the group as entered, so that what the group starts next reaches it, and
     * brings it the group's running quests, handed to the player entering, who is among its
     * players only once in.
     */
    public void enter(@Nonnull String group, @Nonnull World world, @Nonnull UUID playerId) {
        joinedWorlds.computeIfAbsent(group, key -> ConcurrentHashMap.newKeySet()).add(world.getWorldConfig().getUuid());

        for (AbstractQuestProgression<?> quest : indexes.resolve(keyOf(group))) {
            if (QuestProgressionService.get().getLiveQuest(quest.getId()) == null) continue;

            WorldQuestService.get().addQuest(world, quest.getId());
            QuestProgressionService.get().joinQuest(quest, playerId);
        }
    }

    /**
     * @return the worlds of the group entered since they opened and still open.
     */
    @Nonnull
    public List<World> getJoinedWorlds(@Nonnull String group) {
        Set<UUID> worldIds = joinedWorlds.get(group);
        return worldIds == null ? List.of() : WorldQuestService.get().openWorlds(worldIds);
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
     * Brings the quests other servers started in a group to the worlds of it someone is in here.
     *
     * @param added the ids added under each key, as {@link ScopeIndexes#refresh} reports them
     */
    public void spreadAdded(@Nonnull Map<String, Set<UUID>> added) {
        added.forEach((key, questIds) -> {
            if (!key.startsWith(GROUP_INDEX_PREFIX)) return;

            List<World> worlds = getJoinedWorlds(key.substring(GROUP_INDEX_PREFIX.length()));
            for (UUID questId : questIds) {
                for (World world : worlds) WorldQuestService.get().addQuest(world, questId);
            }
        });
    }

    /**
     * Reads the groups named here back now, rather than on the first entry into one of their
     * worlds, with the quests they run.
     */
    public void preload(@Nonnull Collection<String> groupNames) {
        for (String group : groupNames) {
            QuestProgressionService.get().loadQuests(getQuestIds(group));
        }
    }
}
