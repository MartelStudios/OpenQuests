package com.martelstudios.openquests.extension.quests.movement;

import com.martelstudios.openquests.core.persistence.CodecJson;
import com.martelstudios.openquests.extension.quests.movement.TravelQuestAsset.Measure;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TravelMeasureTest {

    @Test
    void aPaceIsCountedInMetresUnlessTheAssetAsksForTime() {
        SprintQuestAsset distance = CodecJson.decode(SprintQuestAsset.CODEC, "{ \"TargetQuantity\": 50 }", "asset");
        SprintQuestAsset duration = CodecJson.decode(SprintQuestAsset.CODEC, "{ \"TargetQuantity\": 60, \"Measure\": \"Seconds\" }", "asset");

        assertNotNull(distance);
        assertNotNull(duration);
        assertEquals(Measure.METRES, distance.getMeasure());
        assertEquals(Measure.SECONDS, duration.getMeasure());
    }

    @Test
    void metresCountTheGroundCovered() {
        assertEquals(0.4, Measure.METRES.of(0.4, 0.05));
        assertEquals(0, Measure.METRES.of(0, 0.05));
    }

    @Test
    void secondsOnlyCountWhileThePlayerActuallyMoves() {
        assertEquals(0.05, Measure.SECONDS.of(0.4, 0.05));
        assertEquals(0, Measure.SECONDS.of(0, 0.05));
    }
}
