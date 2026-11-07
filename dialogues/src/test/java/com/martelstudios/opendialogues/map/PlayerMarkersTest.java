package com.martelstudios.opendialogues.map;

import com.martelstudios.opendialogues.characters.runtime.CharacterPositions;
import com.martelstudios.opendialogues.characters.runtime.CharacterPositionsAccess;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class PlayerMarkersTest {

    private static final UUID PLAYER = UUID.randomUUID();
    private static final UUID WORLD = UUID.randomUUID();

    private final CharacterMarkerProvider provider = new CharacterMarkerProvider(new CharacterPositionsAccess(null, null), player -> null, id -> null);
    private final AtomicInteger recomputes = new AtomicInteger();

    @Test
    void invalidatingAnUnknownPlayerKeepsNothing() {
        provider.invalidate(PLAYER);
        provider.setMapOpen(PLAYER, false);

        assertNull(provider.entry(PLAYER));
    }

    @Test
    void openingTheMapKeepsThePlayerUntilForgotten() {
        provider.setMapOpen(PLAYER, true);
        assertNotNull(provider.entry(PLAYER));

        provider.forget(PLAYER);
        assertNull(provider.entry(PLAYER));
    }

    @Test
    void oneRecomputeWaitsAtATime() {
        PlayerMarkers markers = entry();

        provider.follow(markers, WORLD, recomputes::incrementAndGet);
        provider.follow(markers, WORLD, recomputes::incrementAndGet);

        assertEquals(1, recomputes.get());
    }

    @Test
    void aFailureWaitsForTheNextInvalidation() {
        PlayerMarkers markers = entry();
        markers.pending.set(true);
        provider.settle(PLAYER, markers, WORLD, () -> {
            throw new IllegalStateException("broken status");
        });

        assertNull(provider.follow(markers, WORLD, recomputes::incrementAndGet));
        assertEquals(0, recomputes.get());

        provider.invalidate(PLAYER);
        provider.follow(markers, WORLD, recomputes::incrementAndGet);
        assertEquals(1, recomputes.get());
    }

    @Test
    void anotherWorldIsToldByUuid() {
        PlayerMarkers markers = entry();
        provider.settle(PLAYER, markers, WORLD, () -> seen(WORLD));

        assertNotNull(provider.follow(markers, WORLD, recomputes::incrementAndGet));
        assertNull(provider.follow(markers, UUID.randomUUID(), recomputes::incrementAndGet));
        assertEquals(1, recomputes.get());
    }

    @Test
    void aForgottenPlayerIsNotWrittenBack() {
        PlayerMarkers markers = entry();
        provider.forget(PLAYER);

        provider.settle(PLAYER, markers, WORLD, () -> seen(WORLD));

        assertNull(markers.seen);
    }

    private PlayerMarkers entry() {
        provider.setMapOpen(PLAYER, true);
        return provider.entry(PLAYER);
    }

    private static CharacterMarkerProvider.Seen seen(UUID world) {
        return new CharacterMarkerProvider.Seen(world, new CharacterPositions(), Set.of(), Map.of());
    }
}
