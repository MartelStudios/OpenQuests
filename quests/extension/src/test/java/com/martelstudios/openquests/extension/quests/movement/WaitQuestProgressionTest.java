package com.martelstudios.openquests.extension.quests.movement;

import com.hypixel.hytale.protocol.MovementStates;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WaitQuestProgressionTest {

    private static class TimedWait extends WaitQuestProgression {
        private Instant time = Instant.EPOCH;

        @Override
        protected Instant now() {
            return time;
        }
    }

    @Test
    void standingStillCountsAndOverlappingSamplesCountOnce() {
        TimedWait quest = new TimedWait();
        MovementStates standing = new MovementStates();

        assertEquals(0, quest.accumulate(standing, 0, 0.5));
        assertEquals(0, quest.accumulate(standing, 0, 0.5));
        quest.time = quest.time.plusMillis(500);
        assertEquals(1, quest.accumulate(standing, 0, 0.5));
    }

    @Test
    void aGapBetweenSamplesDoesNotCount() {
        TimedWait quest = new TimedWait();
        MovementStates standing = new MovementStates();

        assertEquals(0, quest.accumulate(standing, 0, 0.25));
        quest.time = quest.time.plusSeconds(60);
        assertEquals(0, quest.accumulate(standing, 0, 0.25));
        quest.time = quest.time.plusMillis(500);
        assertEquals(1, quest.accumulate(standing, 0, 0.5));
    }
}
