package com.martelstudios.openquests.extension.quests.reachlocation;

import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import static com.martelstudios.openquests.extension.constraints.ConstraintFixtures.decode;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReachPathQuestAssetTest {

    private static final ReachPathQuestAsset PATH = decode(ReachPathQuestAsset.CODEC,
        "{\"Locations\": [{\"Position\": {\"X\": 0.5, \"Y\": 10, \"Z\": 0.5}, \"Radius\": 1.2},"
            + " {\"Position\": {\"X\": 2.5, \"Y\": 11, \"Z\": 0.5}, \"Radius\": 1.2}],"
            + " \"ResetOn\": [\"Landing\"], \"TargetQuantity\": 5}");

    @Test
    void theCountIsTheNumberOfSpots() {
        assertEquals(2, PATH.getTargetQuantity());
    }

    @Test
    void aSpotIsWhereverItsRadiusReaches() {
        assertEquals(0, PATH.spotAt(new Vector3d(1.3, 10, 1.2)));
        assertEquals(1, PATH.spotAt(new Vector3d(2.5, 11, 0.5)));
        assertEquals(-1, PATH.spotAt(new Vector3d(0.5, 10, 2.5)));
    }

    @Test
    void theOrderAndResetsComeFromTheAsset() {
        assertTrue(PATH.isOrdered());
        assertTrue(PATH.resetsOn(ReachPathQuestAsset.Reset.Landing));
        assertEquals(false, PATH.resetsOn(ReachPathQuestAsset.Reset.Skip));
    }
}
