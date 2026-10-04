package com.martelstudios.openquests.core.assignments.scope;

import com.martelstudios.openquests.core.assignments.Occasion;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AssignmentScopeTest {

    private static final String PERIOD = "2026-10-10T18:00:00Z";

    @Test
    void anOccasionWithNeitherPlaceNorTimeHappensOnce() {
        Occasion connection = Occasion.of(UUID.randomUUID(), null, null, null);

        assertEquals("once", PlayerAssignmentScope.occasionKey(connection));
        assertEquals("once", AssignmentScope.timeKey(connection));
    }

    @Test
    void aPeriodTellsHandOutsApartForEveryHolder() {
        Occasion tick = Occasion.of(null, null, null, PERIOD);

        assertEquals("period:" + PERIOD, PlayerAssignmentScope.occasionKey(tick));
        assertEquals("period:" + PERIOD, AssignmentScope.timeKey(tick));
    }
}
