package com.martelstudios.openquests.dialogues.constraints;

import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.codec.util.RawJsonReader;
import com.martelstudios.opendialogues.characters.runtime.CharacterPositions;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NearCharacterConstraintTest {

    private static final Vector3d ORIGIN = new Vector3d(0, 0, 0);

    private static CharacterPositions.Spot spot(double x, double y, double z) {
        return new CharacterPositions.Spot(UUID.randomUUID(), new Vector3d(x, y, z), null);
    }

    private static NearCharacterConstraint decode(String json) throws IOException {
        try (RawJsonReader reader = RawJsonReader.fromBuffer(json.toCharArray())) {
            return NearCharacterConstraint.CODEC.decodeJson(reader, new ExtraInfo());
        }
    }

    @Test
    void spot_inside_radius_counts() {
        assertTrue(NearCharacterConstraint.anyWithin(List.of(spot(10, 0, 0)), ORIGIN, 20));
    }

    @Test
    void spot_on_edge_counts() {
        assertTrue(NearCharacterConstraint.anyWithin(List.of(spot(20, 0, 0)), ORIGIN, 20));
    }

    @Test
    void any_spot_counts() {
        assertTrue(NearCharacterConstraint.anyWithin(List.of(spot(500, 0, 0), spot(0, 5, 0)), ORIGIN, 20));
    }

    @Test
    void all_far_spots_refuse() {
        assertFalse(NearCharacterConstraint.anyWithin(List.of(spot(21, 0, 0)), ORIGIN, 20));
    }

    @Test
    void no_spot_refuses() {
        assertFalse(NearCharacterConstraint.anyWithin(List.of(), ORIGIN, 20));
    }

    @Test
    void decodes_fields() throws IOException {
        NearCharacterConstraint constraint = decode("{\"Character\":\"AdventureQuests_KweebecReplant\",\"Radius\":20}");

        assertEquals("AdventureQuests_KweebecReplant", constraint.getCharacter());
        assertEquals(20.0, constraint.getRadius());
    }
}
