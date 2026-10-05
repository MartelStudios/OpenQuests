package com.martelstudios.openquests.core.stores;

import com.martelstudios.openquests.core.persistence.IndexStore;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestsRecordTest {

    private static final String KEY = "universe";

    private final UUID first = UUID.randomUUID();
    private final UUID second = UUID.randomUUID();
    private final UUID third = UUID.randomUUID();

    @Test
    void onlyWhatChangedSinceTheLastWriteIsWritten() {
        MemoryIndexes storage = new MemoryIndexes();
        QuestsRecord record = new QuestsRecord();
        record.load(Set.of());

        record.register(first);
        assertTrue(record.flush(storage, KEY));
        assertFalse(record.flush(storage, KEY));

        record.unregister(first);
        record.register(second);
        record.flush(storage, KEY);

        assertEquals(Set.of(second), storage.loadIndex(KEY));
        assertEquals(3, storage.writes);
    }

    @Test
    void questsAreWrittenBeforeTheIndexListsThem() {
        MemoryIndexes storage = new MemoryIndexes();
        QuestsRecord record = new QuestsRecord();
        record.load(Set.of(first));

        record.register(second);
        record.unregister(first);

        Set<UUID> listedWhenWritten = new HashSet<>();
        record.flush(storage, KEY, listed -> {
            listedWhenWritten.addAll(storage.loadIndex(KEY));
            assertEquals(Set.of(second), listed);
        });

        assertFalse(listedWhenWritten.contains(second));
        assertEquals(Set.of(second), storage.loadIndex(KEY));
    }

    @Test
    void aWriteNeverDropsWhatAnotherServerAdded() {
        MemoryIndexes storage = new MemoryIndexes();
        QuestsRecord onA = new QuestsRecord();
        onA.load(Set.of());
        QuestsRecord onB = new QuestsRecord();
        onB.load(Set.of());

        onA.register(first);
        onB.register(second);
        onA.flush(storage, KEY);
        onB.flush(storage, KEY);

        assertEquals(Set.of(first, second), storage.loadIndex(KEY));
    }

    @Test
    void takingInAnotherServersChangesKeepsThisOnesUnwritten() {
        QuestsRecord record = new QuestsRecord();
        record.load(Set.of(first, second));

        // Here: one added and one removed, neither written yet
        record.register(third);
        record.unregister(second);

        // There: the first one removed, a new one added
        UUID fromElsewhere = UUID.randomUUID();
        QuestsRecord.Absorbed absorbed = record.absorb(Set.of(second, fromElsewhere));

        assertEquals(Set.of(fromElsewhere), absorbed.added());
        assertEquals(Set.of(first), absorbed.removed());
        assertEquals(Set.of(third, fromElsewhere), record.getAllIds());

        MemoryIndexes storage = new MemoryIndexes();
        storage.addToIndex(KEY, Set.of(second, fromElsewhere));
        record.flush(storage, KEY);

        assertEquals(Set.of(third, fromElsewhere), storage.loadIndex(KEY));
    }

    /**
     * Indexes in memory, counting the writes that reach them.
     */
    private static final class MemoryIndexes implements IndexStore {
        private final Map<String, Set<UUID>> indexes = new HashMap<>();
        private int writes;

        @Override
        public Set<UUID> loadIndex(String indexKey) {
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
    }
}
