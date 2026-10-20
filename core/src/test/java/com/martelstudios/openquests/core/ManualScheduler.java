package com.martelstudios.openquests.core;

import javax.annotation.Nonnull;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Delayed;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Keeps delayed tasks until the test runs them, on its own thread, once the clock says they are
 * due. Only one-shot delays are held; anything else goes to the real pool underneath.
 */
public final class ManualScheduler extends ScheduledThreadPoolExecutor {

    private final Clock clock;
    private final List<Pending> pending = new ArrayList<>();

    public ManualScheduler(@Nonnull Clock clock) {
        super(1);
        this.clock = clock;
    }

    @Nonnull
    @Override
    public synchronized ScheduledFuture<?> schedule(@Nonnull Runnable command, long delay, @Nonnull TimeUnit unit) {
        Pending task = new Pending(command, clock.instant().plusMillis(unit.toMillis(delay)));
        pending.add(task);
        return task;
    }

    /**
     * Runs every task due by now, in the order they fall due, those they schedule included.
     *
     * @return how many ran
     */
    public int runDue() {
        int ran = 0;
        while (true) {
            Pending next;
            synchronized (this) {
                pending.removeIf(Pending::isCancelled);
                next = pending.stream()
                              .filter(task -> !task.at.isAfter(clock.instant()))
                              .min(Comparator.comparing(task -> task.at))
                              .orElse(null);
                if (next == null) return ran;
                pending.remove(next);
            }
            next.run();
            ran++;
        }
    }

    /**
     * @return how many tasks wait, cancelled ones aside.
     */
    public synchronized long waiting() {
        return pending.stream().filter(task -> !task.isCancelled()).count();
    }

    private final class Pending extends FutureTask<Void> implements ScheduledFuture<Void> {
        private final Instant at;

        Pending(@Nonnull Runnable command, @Nonnull Instant at) {
            super(command, null);
            this.at = at;
        }

        @Override
        public long getDelay(@Nonnull TimeUnit unit) {
            return unit.convert(at.toEpochMilli() - clock.millis(), TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(@Nonnull Delayed other) {
            return Long.compare(getDelay(TimeUnit.MILLISECONDS), other.getDelay(TimeUnit.MILLISECONDS));
        }
    }
}
