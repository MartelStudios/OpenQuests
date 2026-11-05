package com.martelstudios.opendialogues.characters;

import com.hypixel.hytale.math.shape.Box;
import com.martelstudios.opendialogues.characters.filters.HitboxClearance;
import org.junit.jupiter.api.Test;

import static com.martelstudios.opendialogues.characters.CharacterFixtures.filter;
import static com.martelstudios.opendialogues.characters.CharacterFixtures.kweebec;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HitboxClearanceTest {

    /**
     * A seedling's hitbox, half a block high, standing on the block at y 10.
     */
    private static final Box SEEDLING = new Box(-0.25, 0, -0.25, 0.25, 0.5, 0.25);

    @Test
    void nothingAroundIsClear() {
        assertTrue(HitboxClearance.clear(4.5, 10, 4.5, SEEDLING, (x, y, z) -> null));
    }

    @Test
    void tallGrassInItsCellBuriesIt() {
        Box[] tallGrass = {new Box(0.1, 0, 0.1, 0.9, 0.9, 0.9)};

        assertFalse(HitboxClearance.clear(4.5, 10, 4.5, SEEDLING, (x, y, z) -> x == 4 && y == 10 && z == 4 ? tallGrass : null));
    }

    @Test
    void shortGrassBelowItsUpperHalfIsClear() {
        Box[] shortGrass = {new Box(0.1, 0, 0.1, 0.9, 0.2, 0.9)};

        assertTrue(HitboxClearance.clear(4.5, 10, 4.5, SEEDLING, (x, y, z) -> x == 4 && y == 10 && z == 4 ? shortGrass : null));
    }

    @Test
    void theGroundItStandsOnOnlyTouches() {
        Box[] full = {new Box(0, 0, 0, 1, 1, 1)};

        assertTrue(HitboxClearance.clear(4.5, 10, 4.5, SEEDLING, (x, y, z) -> y == 9 ? full : null));
    }

    @Test
    void aBlockBesideItIsClear() {
        Box[] full = {new Box(0, 0, 0, 1, 1, 1)};

        assertTrue(HitboxClearance.clear(4.5, 10, 4.5, SEEDLING, (x, y, z) -> x == 5 && y == 10 && z == 4 ? full : null));
    }

    @Test
    void aStraddlingHitboxReadsTheNeighbourCell() {
        Box[] tallGrass = {new Box(0, 0, 0, 0.5, 0.9, 1)};

        assertFalse(HitboxClearance.clear(5.1, 10, 4.5, SEEDLING, (x, y, z) -> x == 5 && y == 10 && z == 4 ? tallGrass : null));
    }

    @Test
    void theFilterHoldsWithoutAStoreToRead() {
        assertTrue(filter("{ \"Type\": \"Unobstructed\" }").test(kweebec("Zone1")));
    }
}
