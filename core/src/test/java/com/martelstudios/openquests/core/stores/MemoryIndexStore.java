package com.martelstudios.openquests.core.stores;

import com.martelstudios.openquests.core.persistence.IndexStore;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Indexes in memory, counting the reads and writes that reach them.
 */
public final class MemoryIndexStore implements IndexStore {
    private final Map<String, Set<UUID>> indexes = new HashMap<>();

    public int reads;
    public int writes;

    @Override
    public Set<UUID> loadIndex(String indexKey) {
        reads++;
        return new HashSet<>(indexes.getOrDefault(indexKey, Set.of()));
    }

    @Override
    public Map<String, Set<UUID>> loadIndexes(Collection<String> indexKeys) {
        Map<String, Set<UUID>> loaded = new HashMap<>();
        for (String indexKey : indexKeys) loaded.put(indexKey, loadIndex(indexKey));
        return loaded;
    }

    @Override
    public void addToIndex(String indexKey, Collection<UUID> questIds) {
        if (questIds.isEmpty()) return;
        writes++;
        indexes.computeIfAbsent(indexKey, key -> new HashSet<>()).addAll(questIds);
    }

    @Override
    public void removeFromIndex(String indexKey, Collection<UUID> questIds) {
        if (questIds.isEmpty()) return;
        writes++;
        indexes.computeIfAbsent(indexKey, key -> new HashSet<>()).removeAll(questIds);
    }

    @Override
    public void deleteIndex(String indexKey) {
        indexes.remove(indexKey);
    }

    public boolean has(String indexKey) {
        return indexes.containsKey(indexKey);
    }
}
