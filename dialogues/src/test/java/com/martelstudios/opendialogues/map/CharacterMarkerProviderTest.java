package com.martelstudios.opendialogues.map;

import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.protocol.packets.worldmap.MapMarker;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.worldmap.markers.MapMarkerBuilder;
import com.hypixel.hytale.server.core.universe.world.worldmap.markers.MarkersCollector;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CharacterMarkerProviderTest {

    private static final UUID WORLD = UUID.randomUUID();
    private static final int PLAYERS = 100;
    private static final int MARKERS = 200;
    private static final int TICKS = 10;

    /**
     * Load check: a full server's second of replays, each player calling about ten times.
     */
    @Test
    void hundredPlayersReplayQuickly() {
        List<MapMarker> markers = new ArrayList<>();
        for (int i = 0; i < MARKERS; i++) markers.add(new MapMarkerBuilder("OpenDialogues:" + i, "Coordinate.png", new Transform(i, 64, i)).build());
        CharacterMarkerProvider.Built built = new CharacterMarkerProvider.Built(WORLD, null, 0, markers, markers.subList(0, 10));
        Counter counter = new Counter();

        for (int warm = 0; warm < PLAYERS; warm++) built.replay(true, counter);
        counter.count = 0;
        long start = System.nanoTime();
        for (int tick = 0; tick < TICKS; tick++) {
            for (int player = 0; player < PLAYERS; player++) built.replay(true, counter);
        }
        long millis = (System.nanoTime() - start) / 1_000_000;

        System.out.printf("Replayed %d markers in %d ms%n", counter.count, millis);
        assertEquals(PLAYERS * MARKERS * TICKS, counter.count);
        assertTrue(millis < 200, "replays took " + millis + " ms");
    }

    @Test
    void closedMapReplaysCompassMarkersOnly() {
        MapMarker npc = new MapMarkerBuilder("OpenDialogues:a", "Coordinate.png", new Transform(0, 0, 0)).build();
        MapMarker target = new MapMarkerBuilder("OpenDialogues:b", "Coordinate.png", new Transform(0, 0, 0)).build();
        CharacterMarkerProvider.Built built = new CharacterMarkerProvider.Built(WORLD, null, 0, List.of(npc, target), List.of(target));
        Counter counter = new Counter();

        built.replay(false, counter);

        assertEquals(1, counter.count);
    }

    private static final class Counter implements MarkersCollector {
        int count;

        @Override
        public void add(MapMarker marker) {
            count++;
        }

        @Override
        public void addIgnoreViewDistance(MapMarker marker) {
            count++;
        }

        @Override
        public Predicate<PlayerRef> getPlayerMapFilter() {
            return player -> true;
        }

        @Override
        public boolean isInViewDistance(double x, double z) {
            return true;
        }
    }
}
