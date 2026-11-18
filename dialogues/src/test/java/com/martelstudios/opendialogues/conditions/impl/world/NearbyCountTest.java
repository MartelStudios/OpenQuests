package com.martelstudios.opendialogues.conditions.impl.world;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NearbyCountTest {

    @Test
    void stopsAtMinWithoutMax() {
        AtomicInteger calls = new AtomicInteger();

        assertTrue(NearbyCount.holds(Collections.nCopies(10, true), counting(calls), 3, null));
        assertEquals(3, calls.get());
    }

    @Test
    void maxZeroFailsOnFirstMatch() {
        AtomicInteger calls = new AtomicInteger();

        assertFalse(NearbyCount.holds(List.of(true, true, true), counting(calls), 0, 0));
        assertEquals(1, calls.get());
    }

    @Test
    void maxZeroHoldsOnNone() {
        assertTrue(NearbyCount.holds(List.<Boolean>of(), match -> match, 0, 0));
    }

    @Test
    void countAboveMaxFails() {
        assertFalse(NearbyCount.holds(List.of(true, true, true), match -> match, 1, 2));
    }

    @Test
    void countBelowMinFails() {
        assertFalse(NearbyCount.holds(List.of(true, false, true), match -> match, 3, null));
    }

    private static Predicate<Boolean> counting(AtomicInteger calls) {
        return match -> {
            calls.incrementAndGet();
            return match;
        };
    }
}
