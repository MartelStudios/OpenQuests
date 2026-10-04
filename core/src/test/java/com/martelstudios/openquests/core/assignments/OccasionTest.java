package com.martelstudios.openquests.core.assignments;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OccasionTest {

    private static final String PERIOD = "2026-10-10T18:00:00Z";

    // Keying an occasion never reads the player's components, so none are needed here
    private static final Occasion CONNECTION = Occasion.connection(UUID.randomUUID(), null);

    @Test
    void anOccasionWithNeitherPlaceNorTimeHappensOnce() {
        assertFalse(CONNECTION.isTimed());
        assertEquals("once", CONNECTION.timeKey());
        assertEquals("once", CONNECTION.placeKey());
    }

    @Test
    void aPeriodTellsHandOutsApartForEveryHolder() {
        Occasion tick = Occasion.period(PERIOD);

        assertTrue(tick.isTimed());
        assertEquals("period:" + PERIOD, tick.timeKey());
        assertEquals("period:" + PERIOD, tick.placeKey());
        assertNull(tick.getPlayerId());
        assertNull(tick.getPlayer());
    }

    @Test
    void aConnectionDuringAPeriodKeepsItsPlayer() {
        Occasion during = CONNECTION.during(PERIOD);

        assertEquals(CONNECTION.getPlayerId(), during.getPlayerId());
        assertEquals("period:" + PERIOD, during.placeKey());
    }
}
