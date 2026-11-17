package com.martelstudios.openquests.core.scopes;

import com.martelstudios.openquests.core.scopes.team.TeamQuestService;
import com.martelstudios.openquests.core.scopes.universe.UniverseQuestService;
import com.martelstudios.openquests.core.scopes.world.WorldGroupIndex;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Follows the indexes other servers write too, the universe's, those of the world groups this
 * server holds and those of the teams read here, and brings in the quests they added.
 */
public final class ScopeIndexFollower {

    private final ScopeIndexes indexes;
    private final UniverseQuestService universe;
    private final WorldGroupIndex worldGroups;
    private final TeamQuestService teams;

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
        keys.addAll(indexes.getLoadedKeys(TeamQuestService.TEAM_INDEX_PREFIX));
        keys.add(UniverseQuestService.UNIVERSE_INDEX_KEY);

        Map<String, Set<UUID>> added = indexes.refresh(keys);

        universe.joinAdded(added.getOrDefault(UniverseQuestService.UNIVERSE_INDEX_KEY, Set.of()));
        worldGroups.spreadAdded(added);

        added.forEach((key, questIds) -> {
            UUID teamId = TeamQuestService.parseKey(key);
            if (teamId != null) teams.joinAdded(teamId, questIds);
        });
    }
}
