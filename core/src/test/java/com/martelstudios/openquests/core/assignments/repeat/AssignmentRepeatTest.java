package com.martelstudios.openquests.core.assignments.repeat;

import com.martelstudios.openquests.core.models.AssignmentRecord;
import org.junit.jupiter.api.Test;

import static com.martelstudios.openquests.core.assignments.repeat.AssignmentRepeat.Decision.HAND_OUT;
import static com.martelstudios.openquests.core.assignments.repeat.AssignmentRepeat.Decision.REPLACE;
import static com.martelstudios.openquests.core.assignments.repeat.AssignmentRepeat.Decision.SKIP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AssignmentRepeatTest {

    private static final String WEEK_ONE = "period:2026-10-10T18:00:00Z";
    private static final String WEEK_TWO = "period:2026-10-17T18:00:00Z";

    @Test
    void onceHandsAnOccasionOutASingleTime() {
        OnceRepeat once = new OnceRepeat();

        assertEquals(HAND_OUT, once.decide(null, "once", false, false, false));
        assertEquals(SKIP, once.decide(handed(1, "once"), "once", false, true, false));
    }

    @Test
    void onceHandsEveryNewWorldOrPeriodOut() {
        OnceRepeat once = new OnceRepeat();

        assertEquals(HAND_OUT, once.decide(handed(1, WEEK_ONE), WEEK_TWO, true, false, true));
    }

    @Test
    void afterEndWaitsForTheLineToEnd() {
        AfterEndRepeat afterEnd = new AfterEndRepeat();

        assertEquals(SKIP, afterEnd.decide(handed(1, "once"), "once", false, true, true));
        assertEquals(HAND_OUT, afterEnd.decide(handed(1, "once"), "once", false, true, false));
    }

    @Test
    void afterEndHandsAPeriodOutOnceEvenWhenItsLineEndedEarly() {
        AfterEndRepeat afterEnd = new AfterEndRepeat();

        assertEquals(SKIP, afterEnd.decide(handed(1, WEEK_ONE), WEEK_ONE, true, true, false));
        assertEquals(HAND_OUT, afterEnd.decide(handed(1, WEEK_ONE), WEEK_TWO, true, false, false));
    }

    @Test
    void replaceStartsAnewAndFailsWhatIsStillRunning() {
        ReplaceRepeat replace = new ReplaceRepeat();

        assertEquals(REPLACE, replace.decide(handed(1, WEEK_ONE), WEEK_TWO, true, false, true));
        assertEquals(HAND_OUT, replace.decide(handed(1, WEEK_ONE), WEEK_TWO, true, false, false));
        assertEquals(SKIP, replace.decide(handed(2, WEEK_TWO), WEEK_TWO, true, true, true));
    }

    @Test
    void theCapStopsEveryKind() {
        AfterEndRepeat afterEnd = new AfterEndRepeat();
        afterEnd.max = 7;

        assertEquals(HAND_OUT, afterEnd.decide(handed(6, "once"), "once", false, true, false));
        assertEquals(SKIP, afterEnd.decide(handed(7, "once"), "once", false, true, false));
    }

    @Test
    void aNegativeCapIsRefused() {
        ReplaceRepeat replace = new ReplaceRepeat();
        replace.max = -1;

        assertNotNull(replace.findInconsistency());
    }

    private static AssignmentRecord handed(int count, String occasion) {
        return new AssignmentRecord(count, 1790476800000L, occasion);
    }
}
