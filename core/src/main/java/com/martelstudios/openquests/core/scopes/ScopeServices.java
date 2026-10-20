package com.martelstudios.openquests.core.scopes;

import com.martelstudios.openquests.core.scopes.universe.UniverseQuestService;
import com.martelstudios.openquests.core.scopes.world.WorldGroupIndex;
import com.martelstudios.openquests.core.scopes.world.WorldQuestService;

import javax.annotation.Nonnull;

/**
 * The services a quest scope shares, indexes and releases quests through, handed to it at each call
 * since a scope is read back with its quest rather than built by the services.
 *
 * @param universe the quests the whole server shares
 * @param worlds the quests of each world
 * @param worldGroups the quests of each group of worlds
 */
public record ScopeServices(@Nonnull UniverseQuestService universe,
                            @Nonnull WorldQuestService worlds,
                            @Nonnull WorldGroupIndex worldGroups) {}
