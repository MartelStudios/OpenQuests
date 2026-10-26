package com.martelstudios.openquests.core.persistence;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Named sets of running quests, which is how a scope remembers what it shares: one key for the
 * universe, one per world, one per group of worlds. Written member by member, never replaced
 * whole, so servers sharing a key never drop what another added.
 */
public interface IndexStore {

    /**
     * @return the ids under that key, empty for a key nothing answers to.
     */
    @Nonnull
    Set<UUID> loadIndex(@Nonnull String indexKey);

    /**
     * @return the ids under each of those keys, every key asked for answering, if only with
     * nothing.
     */
    @Nonnull
    Map<String, Set<UUID>> loadIndexes(@Nonnull Collection<String> indexKeys);

    /**
     * Adds quests under a key; one already there stays as it is.
     */
    void addToIndex(@Nonnull String indexKey, @Nonnull Collection<UUID> questIds);

    /**
     * Takes quests out from under a key; one not there is no matter.
     */
    void removeFromIndex(@Nonnull String indexKey, @Nonnull Collection<UUID> questIds);

    /**
     * Lets go of a key nothing will read again, such as a closed instance's.
     */
    void deleteIndex(@Nonnull String indexKey);
}
