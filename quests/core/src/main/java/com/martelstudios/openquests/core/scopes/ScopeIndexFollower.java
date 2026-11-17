package com.martelstudios.openquests.core.scopes;

import com.hypixel.hytale.logger.HytaleLogger;
import com.martelstudios.openquests.core.scopes.team.TeamQuestService;
import com.martelstudios.openquests.core.scopes.universe.UniverseQuestService;
import com.martelstudios.openquests.core.scopes.world.WorldGroupIndex;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Follows the indexes other servers write too, the universe's, those of the world groups this
 * server holds and those of the teams read here, and brings in the quests they added.
 */
public final class ScopeIndexFollower {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final ScopeIndexes indexes;
    private final UniverseQuestService universe;
    private final WorldGroupIndex worldGroups;
    private final TeamQuestService teams;

    /**
     * The quests a team failed to take in, absorbed by its index already: handed again next time.
     * Only touched on the storage thread.
     */
    private final Map<String, Set<UUID>> retry = new HashMap<>();

    public ScopeIndexFollower(@Nonnull ScopeIndexes indexes, @Nonnull UniverseQuestService universe, @Nonnull WorldGroupIndex worldGroups, @Nonnull TeamQuestService teams) {
        this.indexes = indexes;
        this.universe = universe;
        this.worldGroups = worldGroups;
        this.teams = teams;
    }

    /**
     * Reads every followed index in one go, then hands what was added to the scope it belongs to.
     */
    public void follow() {
        Set<String> keys = new HashSet<>(indexes.getLoadedKeys(WorldGroupIndex.GROUP_INDEX_PREFIX));
        Set<String> teamKeys = indexes.getLoadedKeys(TeamQuestService.TEAM_INDEX_PREFIX);
        keys.addAll(teamKeys);
        keys.add(UniverseQuestService.UNIVERSE_INDEX_KEY);

        Map<String, Set<UUID>> added = indexes.refresh(keys);

        universe.joinAdded(added.getOrDefault(UniverseQuestService.UNIVERSE_INDEX_KEY, Set.of()));
        worldGroups.spreadAdded(added);

        Map<String, Set<UUID>> toJoin = new HashMap<>();
        added.forEach((key, questIds) -> toJoin.computeIfAbsent(key, k -> new HashSet<>()).addAll(questIds));
        // A team no longer read here is taken back in full by its next reader
        retry.forEach((key, questIds) -> {
            if (teamKeys.contains(key)) toJoin.computeIfAbsent(key, k -> new HashSet<>()).addAll(questIds);
        });
        retry.clear();

        toJoin.forEach((key, questIds) -> {
            UUID teamId = TeamQuestService.parseKey(key);
            if (teamId == null) return;

            try {
                teams.joinAdded(teamId, questIds);
            } catch (RuntimeException e) {
                LOGGER.atWarning().withCause(e).log("Failed to bring in the quests team %s was shared, trying again next time", teamId);
                retry.put(key, questIds);
            }
        });
    }
}
