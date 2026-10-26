package com.martelstudios.opendialogues.sessions;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SessionStoreTest {

    private final UUID player = UUID.randomUUID();
    private final SessionStore<String> store = new SessionStore<>();

    @Test
    void resolvesTheOnlyOpenDialogueWithoutAName() {
        store.put(player, "a", "session a");

        assertEquals("session a", store.resolve(player, null));
    }

    @Test
    void needsANameWhenSeveralAreOpen() {
        store.put(player, "a", "session a");
        store.put(player, "b", "session b");

        assertNull(store.resolve(player, null));
        assertEquals("session b", store.resolve(player, "b"));
        assertNull(store.resolve(player, "c"));
    }

    @Test
    void reopeningMovesADialogueLast() {
        store.put(player, "a", "first a");
        store.put(player, "b", "session b");
        store.put(player, "a", "second a");

        assertEquals(List.of("b", "a"), store.openIds(player));
        assertEquals("second a", store.resolve(player, "a"));
    }

    @Test
    void forgetsAPlayerOnceTheirLastDialogueCloses() {
        store.put(player, "a", "session a");

        assertEquals("session a", store.remove(player, "a"));
        assertNull(store.remove(player, "a"));
        assertEquals(List.of(), store.openIds(player));
        assertEquals(List.of(), store.removeAll(player));
    }
}
