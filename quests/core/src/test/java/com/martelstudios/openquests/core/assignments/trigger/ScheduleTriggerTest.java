package com.martelstudios.openquests.core.assignments.trigger;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ScheduleTriggerTest {

    private static final Instant FIRST = Instant.parse("2026-10-10T18:00:00Z");

    @Test
    void nothingIsDueBeforeTheFirstDate() {
        ScheduleTrigger weekly = new ScheduleTrigger("2026-10-10T18:00:00Z", "P7D");

        assertNull(weekly.currentPeriod(FIRST.minusSeconds(1)));
        assertNull(weekly.onTick(FIRST.minusSeconds(1)));
    }

    @Test
    void aPeriodLastsUntilTheNextOneBegins() {
        ScheduleTrigger weekly = new ScheduleTrigger("2026-10-10T18:00:00Z", "P7D");

        assertEquals(FIRST, weekly.currentPeriod(FIRST));
        assertEquals(FIRST, weekly.currentPeriod(Instant.parse("2026-10-17T17:59:59Z")));
        assertEquals(Instant.parse("2026-10-17T18:00:00Z"), weekly.currentPeriod(Instant.parse("2026-10-17T18:00:00Z")));
    }

    @Test
    void anAbsenceOfMonthsLandsOnTheCurrentPeriodOnly() {
        ScheduleTrigger weekly = new ScheduleTrigger("2026-10-10T18:00:00Z", "P7D");

        assertEquals(Instant.parse("2026-12-05T18:00:00Z"), weekly.currentPeriod(Instant.parse("2026-12-10T09:00:00Z")));
    }

    @Test
    void aSingleDateLastsForGood() {
        ScheduleTrigger christmas = new ScheduleTrigger("2026-12-24T18:00:00+01:00", null);

        assertEquals(Instant.parse("2026-12-24T17:00:00Z"), christmas.currentPeriod(Instant.parse("2027-03-01T00:00:00Z")));
    }

    @Test
    void aTickConcernsNobodyInParticular() {
        ScheduleTrigger weekly = new ScheduleTrigger("2026-10-10T18:00:00Z", "P7D");

        assertEquals("2026-10-10T18:00:00Z", weekly.onTick(FIRST.plusSeconds(60)).getPeriod());
        assertNull(weekly.onTick(FIRST.plusSeconds(60)).getPlayerId());
    }

    @Test
    void malformedDatesAndDurationsAreRefused() {
        assertNotNull(new ScheduleTrigger("next saturday", null).findInconsistency());
        assertNotNull(new ScheduleTrigger("2026-10-10T18:00:00Z", "weekly").findInconsistency());
        assertNotNull(new ScheduleTrigger("2026-10-10T18:00:00Z", "PT0S").findInconsistency());
        assertNull(new ScheduleTrigger("2026-10-10T18:00:00Z", "PT12H").findInconsistency());
    }
}
