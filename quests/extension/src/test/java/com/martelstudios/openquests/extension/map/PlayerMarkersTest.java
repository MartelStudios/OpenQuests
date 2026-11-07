package com.martelstudios.openquests.extension.map;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class PlayerMarkersTest {

    private static final UUID PLAYER = UUID.randomUUID();
    private static final UUID WORLD = UUID.randomUUID();

    private final QuestTargetMarkerProvider provider = new QuestTargetMarkerProvider(null, player -> null);
    private final AtomicInteger builds = new AtomicInteger();

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
    void builtAgainOnlyOnInvalidationOrAnotherWorld() {
        provider.setMapOpen(PLAYER, true);
        PlayerMarkers markers = provider.entry(PLAYER);

        follow(markers, WORLD);
        follow(markers, WORLD);
        assertEquals(1, builds.get());

        provider.invalidate(PLAYER);
        follow(markers, WORLD);
        assertEquals(2, builds.get());

        follow(markers, UUID.randomUUID());
        assertEquals(3, builds.get());
    }

    private void follow(PlayerMarkers markers, UUID world) {
        provider.follow(markers, world, () -> {
            builds.incrementAndGet();
            return new QuestTargetMarkerProvider.Built(world, List.of(), List.of());
        });
    }
}
