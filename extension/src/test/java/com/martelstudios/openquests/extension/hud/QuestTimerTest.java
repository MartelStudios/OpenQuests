package com.martelstudios.openquests.extension.hud;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuestTimerTest {

    private static final Instant START = Instant.parse("2026-09-27T12:00:00Z");

    private static final QuestTimer THREE_MINUTES = new QuestTimer(START, START.plus(Duration.ofMinutes(3)));

    @Test
    void theBarEmptiesAsTheTimeRunsOut() {
        assertEquals(1f, THREE_MINUTES.fractionLeft(START));
        assertEquals(0.5f, THREE_MINUTES.fractionLeft(START.plusSeconds(90)), 0.0001f);
        assertEquals(0f, THREE_MINUTES.fractionLeft(START.plusSeconds(200)));
    }

    @Test
    void aQuestWithNoRecordedStartIsDrawnFull() {
        QuestTimer timer = new QuestTimer(null, START.plusSeconds(60));

        assertEquals(1f, timer.fractionLeft(START.plusSeconds(30)));
    }

    @Test
    void theClockOnlyReadsZeroOnceTheTimeIsGone() {
        assertEquals(1, THREE_MINUTES.secondsLeft(START.plus(Duration.ofMillis(179_001))));
        assertEquals(0, THREE_MINUTES.secondsLeft(START.plusSeconds(180)));
        assertEquals(0, THREE_MINUTES.secondsLeft(START.plusSeconds(500)));
    }

    @Test
    void theClockShowsMinutesAndSecondsUnderAnHour() {
        assertEquals("2:47", THREE_MINUTES.format(START.plusSeconds(13)).getRawText());
        assertEquals("0:05", THREE_MINUTES.format(START.plusSeconds(175)).getRawText());
    }

    @Test
    void theClockShowsHoursUnderADay() {
        QuestTimer timer = new QuestTimer(START, START.plus(Duration.ofHours(20)));

        assertEquals("19:59:30", timer.format(START.plusSeconds(30)).getRawText());
    }

    @Test
    void aTimerCountedInDaysIsRedrawnEveryMinute() {
        QuestTimer timer = new QuestTimer(START, START.plus(Duration.ofDays(3)));

        assertEquals(60_000, timer.refreshMillis(START));
        assertEquals(1_000, THREE_MINUTES.refreshMillis(START));
    }
}
