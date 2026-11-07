package com.martelstudios.opendialogues.map;

import com.martelstudios.opendialogues.characters.runtime.CharacterPositionsAccess;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerMarkersTest {

    private static final UUID PLAYER = UUID.randomUUID();

    private final CharacterMarkerProvider provider = new CharacterMarkerProvider(new CharacterPositionsAccess(null, null), player -> null, id -> null);

    @Test
    void invalidatingAnUnknownPlayerKeepsNothing() {
        provider.invalidate(PLAYER);
        provider.setMapOpen(PLAYER, false);

        assertFalse(provider.knows(PLAYER));
    }

    @Test
    void openingTheMapKeepsThePlayerUntilForgotten() {
        provider.setMapOpen(PLAYER, true);
        assertTrue(provider.knows(PLAYER));

        provider.forget(PLAYER);
        assertFalse(provider.knows(PLAYER));
    }
}
