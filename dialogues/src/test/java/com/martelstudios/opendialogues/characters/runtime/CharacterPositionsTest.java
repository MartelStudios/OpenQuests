package com.martelstudios.opendialogues.characters.runtime;

import com.martelstudios.opendialogues.CodecJson;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CharacterPositionsTest {

    private static final UUID NPC = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void viewIsSharedUntilTheVersionMoves() {
        CharacterPositions positions = new CharacterPositions();
        positions.record("farmer", NPC, new Vector3d(), null);
        Map<String, java.util.Collection<CharacterPositions.Spot>> first = positions.view();

        assertSame(first, positions.view());
        positions.record("farmer", NPC, new Vector3d(5, 0, 0), null);
        assertNotSame(first, positions.view());
    }

    @Test
    void savedPositionsReloadWithTheirGenerations() {
        CharacterPositions positions = new CharacterPositions();
        positions.catchUp(1, 2);
        positions.record("farmer", NPC, new Vector3d(1, 2, 3), "names.sorrel");
        positions.record("mage", new UUID(0, 2), new Vector3d(4, 5, 6), null);

        CharacterPositions reloaded = CodecJson.roundTrip(CharacterPositions.CODEC, positions);
        reloaded.catchUp(1, 2);

        assertEquals(positions.view(), reloaded.view());
    }

    @Test
    void releaseForgetsTheNpc() {
        CharacterPositions positions = new CharacterPositions();
        positions.record("farmer", NPC, new Vector3d(1, 2, 3), "names.sorrel");

        positions.forget(NPC);

        assertTrue(positions.view().isEmpty());
    }

    @Test
    void recordMovesAnNpcBetweenCharacters() {
        CharacterPositions positions = new CharacterPositions();
        positions.record("farmer", NPC, new Vector3d(1, 2, 3), null);
        positions.record("mage", NPC, new Vector3d(4, 5, 6), null);

        Map<String, ? extends java.util.Collection<CharacterPositions.Spot>> view = positions.view();
        assertFalse(view.containsKey("farmer"));
        assertEquals(new Vector3d(4, 5, 6), view.get("mage").iterator().next().position());
    }

    @Test
    void catchUpClearsWhenBehind() {
        CharacterPositions positions = new CharacterPositions();
        positions.catchUp(1, 1);
        positions.record("farmer", NPC, new Vector3d(), null);

        positions.catchUp(1, 1);
        assertFalse(positions.view().isEmpty());
        positions.catchUp(1, 2);
        assertTrue(positions.view().isEmpty());

        positions.record("farmer", NPC, new Vector3d(), null);
        positions.catchUp(2, 2);
        assertTrue(positions.view().isEmpty());
    }

    @Test
    void versionGrowsOnChangeOnly() {
        CharacterPositions positions = new CharacterPositions();
        int before = positions.version();
        positions.record("farmer", NPC, new Vector3d(1, 2, 3), null);
        int recorded = positions.version();
        positions.record("farmer", NPC, new Vector3d(1, 2, 3), null);

        assertTrue(recorded > before);
        assertEquals(recorded, positions.version());
    }
}
