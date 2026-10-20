package com.martelstudios.openquests.core;

import javax.annotation.Nonnull;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * A clock that only moves when the test moves it, starting at the real time so it agrees with the
 * dates the quests take on their own.
 */
public final class TestClock extends Clock {

    private volatile Instant now = Instant.now();

    /**
     * Moves the time forward.
     */
    public void advance(@Nonnull Duration duration) {
        now = now.plus(duration);
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return now;
    }
}
