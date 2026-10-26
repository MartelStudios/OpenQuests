package com.martelstudios.openquests.core.stores;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
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
        MemoryIndexStore storage = new MemoryIndexStore();
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
        MemoryIndexStore storage = new MemoryIndexStore();
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
        MemoryIndexStore storage = new MemoryIndexStore();
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

        MemoryIndexStore storage = new MemoryIndexStore();
        storage.addToIndex(KEY, Set.of(second, fromElsewhere));
        record.flush(storage, KEY);

        assertEquals(Set.of(third, fromElsewhere), storage.loadIndex(KEY));
    }
}
