package com.martelstudios.openquests.core.scopes;

import com.martelstudios.openquests.core.scopes.universe.UniverseQuestService;
import com.martelstudios.openquests.core.scopes.world.WorldGroupIndex;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Follows the indexes other servers write too, the universe's and those of the world groups this
 * server holds, and brings in the quests they added.
 */
public final class ScopeIndexFollower {

    private final ScopeIndexes indexes;
    private final UniverseQuestService universe;
    private final WorldGroupIndex worldGroups;

    public ScopeIndexFollower(@Nonnull ScopeIndexes indexes, @Nonnull UniverseQuestService universe, @Nonnull WorldGroupIndex worldGroups) {
        this.indexes = indexes;
        this.universe = universe;
        this.worldGroups = worldGroups;
    }

    /**
     * Reads every followed index in one go, then hands what was added to the scope it belongs to.
     */
    public void follow() {
        Set<String> keys = new HashSet<>(indexes.getLoadedKeys(WorldGroupIndex.GROUP_INDEX_PREFIX));
        keys.add(UniverseQuestService.UNIVERSE_INDEX_KEY);

        Map<String, Set<UUID>> added = indexes.refresh(keys);

        universe.joinAdded(added.getOrDefault(UniverseQuestService.UNIVERSE_INDEX_KEY, Set.of()));
        worldGroups.spreadAdded(added);
    }
}
