package com.martelstudios.openquests.core.scopes;

import com.martelstudios.openquests.core.stores.MemoryIndexStore;
import com.martelstudios.openquests.core.stores.QuestsRecord;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScopeIndexesTest {

    private static final String UNIVERSE = "universe";
    private static final String GROUP = "worlds:Arena";

    private final MemoryIndexStore storage = new MemoryIndexStore();
    private final ScopeIndexes indexes = new ScopeIndexes(storage, new WritingAtOnce(storage));

    private final UUID first = UUID.randomUUID();
    private final UUID second = UUID.randomUUID();

    @Test
    void anIndexIsReadBackOnceAndWrittenAsItChanges() {
        storage.addToIndex(UNIVERSE, Set.of(first));
        storage.writes = 0;

        assertEquals(Set.of(first), indexes.getIds(UNIVERSE));
        assertTrue(indexes.add(UNIVERSE, second));
        assertFalse(indexes.add(UNIVERSE, second));
        assertTrue(indexes.remove(UNIVERSE, first));

        assertEquals(1, storage.reads);
        assertEquals(2, storage.writes);
        assertEquals(Set.of(second), storage.loadIndex(UNIVERSE));

        // Everything is written already, so the save pass has nothing left to write
        indexes.flushAll();
        assertEquals(2, storage.writes);
    }

    @Test
    void onlyTheIndexesReadHereAreFollowed() {
        indexes.getIds(UNIVERSE);
        storage.addToIndex(UNIVERSE, Set.of(first));
        storage.addToIndex(GROUP, Set.of(second));

        Map<String, Set<UUID>> added = indexes.refresh(Set.of(UNIVERSE, GROUP));

        assertEquals(Map.of(UNIVERSE, Set.of(first)), added);
        assertEquals(Set.of(UNIVERSE), indexes.getLoadedKeys(""));
        assertTrue(indexes.refresh(Set.of(UNIVERSE)).isEmpty());
    }

    @Test
    void anIndexLetGoOfLeavesNothingBehind() {
        indexes.add(GROUP, first);

        indexes.delete(GROUP);

        assertTrue(indexes.getLoadedKeys("worlds:").isEmpty());
        assertFalse(storage.has(GROUP));
    }

    /**
     * Writes on the caller's thread, which is all a test needs of the storage thread.
     */
    private record WritingAtOnce(@Nonnull MemoryIndexStore storage) implements ScopeIndexes.Writer {
        @Override
        public void flushIndex(@Nonnull QuestsRecord record, @Nonnull String key) {
            record.flush(storage, key);
        }

        @Override
        public void deleteIndex(@Nonnull String key) {
            storage.deleteIndex(key);
        }
    }
}
