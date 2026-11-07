package com.martelstudios.opendialogues.map;

import com.hypixel.hytale.protocol.packets.worldmap.MapMarker;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.worldmap.markers.MarkersCollector;
import com.martelstudios.opendialogues.characters.runtime.CharacterPositions;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CharacterMarkerProviderTest {

    private static final UUID WORLD = UUID.randomUUID();
    private static final int PLAYERS = 100;
    private static final int NPCS = 200;
    private static final int CHARACTERS = 20;
    private static final int MOVES = 10;

    /**
     * Load check: every NPC move makes each player join the positions again, with no status or
     * history asked. The bound is loose, as CI machines vary: it catches a cost gone quadratic.
     */
    @Test
    void hundredPlayersFollowNpcMovesQuickly() {
        CharacterPositions positions = new CharacterPositions();
        for (int npc = 0; npc < NPCS; npc++) positions.record("character" + npc % CHARACTERS, new UUID(0, npc), new Vector3d(npc, 64, npc), null);
        Map<String, CharacterMarkerProvider.Look> shown = new HashMap<>();
        for (int character = 0; character < CHARACTERS; character++) shown.put("character" + character, new CharacterMarkerProvider.Look(new CharacterMarkerRule.Shown("Coordinate.png", character == 0), null));
        CharacterMarkerProvider.Seen seen = new CharacterMarkerProvider.Seen(WORLD, positions, Set.copyOf(shown.keySet()), shown);
        Counter counter = new Counter();

        long start = System.nanoTime();
        for (int move = 0; move < MOVES; move++) {
            positions.record("character0", new UUID(0, 0), new Vector3d(move * 10, 64, 0), null);
            for (int player = 0; player < PLAYERS; player++) CharacterMarkerProvider.join(seen, positions.version(), positions.view()).replay(true, counter);
        }
        long millis = (System.nanoTime() - start) / 1_000_000;

        System.out.printf("Joined and replayed %d markers in %d ms%n", counter.count, millis);
        assertEquals(PLAYERS * NPCS * MOVES, counter.count);
        assertTrue(millis < 10_000, "joins took " + millis + " ms");
    }

    @Test
    void closedMapReplaysCompassMarkersOnly() {
        CharacterPositions positions = new CharacterPositions();
        positions.record("farmer", new UUID(0, 1), new Vector3d(), null);
        positions.record("mage", new UUID(0, 2), new Vector3d(), null);
        Map<String, CharacterMarkerProvider.Look> shown = Map.of(
            "farmer", new CharacterMarkerProvider.Look(new CharacterMarkerRule.Shown("Coordinate.png", false), null),
            "mage", new CharacterMarkerProvider.Look(new CharacterMarkerRule.Shown("TurnIn.png", true), null));
        CharacterMarkerProvider.Built built = CharacterMarkerProvider.join(new CharacterMarkerProvider.Seen(WORLD, positions, shown.keySet(), shown), positions.version(), positions.view());
        Counter counter = new Counter();

        built.replay(false, counter);

        assertEquals(1, counter.count);
    }

    static final class Counter implements MarkersCollector {
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
