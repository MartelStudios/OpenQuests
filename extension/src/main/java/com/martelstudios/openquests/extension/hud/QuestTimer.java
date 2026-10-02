package com.martelstudios.openquests.extension.hud;

import com.hypixel.hytale.server.core.Message;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.services.QuestDeadlineService;
import com.martelstudios.openquests.extension.journal.QuestPageRows;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;

/**
 * The time a running quest has left, as the tracker draws it: a bar and a clock. Read from the
 * core's deadline, so any constraint that ends a quest in time shows up without the panel knowing it.
 *
 * @param start when the quest was handed out, {@code null} for one that never recorded it
 * @param end   when a constraint of its asset ends it
 */
public record QuestTimer(@Nullable Instant start, @Nonnull Instant end) {

    private static final long MINUTE_SECONDS = 60;
    private static final long HOUR_SECONDS = 3_600;
    private static final long DAY_SECONDS = 86_400;

    /**
     * @return the timer of a quest still running against a deadline, {@code null} for any other.
     */
    @Nullable
    public static QuestTimer of(@Nonnull AbstractQuestProgression<?> quest) {
        if (quest.isCompleted()) return null;

        Instant end = QuestDeadlineService.getDeadline(quest);
        return end == null ? null : new QuestTimer(quest.getStartedAt(), end);
    }

    /**
     * Rounded up, so the clock only reads zero once the time is actually gone.
     */
    public long secondsLeft(@Nonnull Instant now) {
        long millis = Duration.between(now, end).toMillis();
        return millis <= 0 ? 0 : (millis + 999) / 1_000;
    }

    /**
     * @return 1 when the quest was handed out, 0 once it runs out. Full for a quest with no
     * recorded start, which is better drawn whole than guessed at.
     */
    public float fractionLeft(@Nonnull Instant now) {
        if (start == null) return 1f;

        long total = Duration.between(start, end).toMillis();
        if (total <= 0) return 0f;

        float left = (float) Duration.between(now, end).toMillis() / total;
        return Math.max(0f, Math.min(1f, left));
    }

    /**
     * A clock while there is less than a day left, where every second counts. Beyond that, the
     * journal's count of days and hours, since a clock of hundreds of hours reads as nothing.
     */
    @Nonnull
    public Message format(@Nonnull Instant now) {
        long seconds = secondsLeft(now);
        if (seconds >= DAY_SECONDS) return QuestPageRows.formatDuration(Duration.ofSeconds(seconds));

        long hours = seconds / HOUR_SECONDS;
        long minutes = seconds % HOUR_SECONDS / MINUTE_SECONDS;
        long rest = seconds % MINUTE_SECONDS;

        String clock = hours > 0 ? String.format("%d:%02d:%02d", hours, minutes, rest) : String.format("%d:%02d", minutes, rest);
        return Message.raw(clock);
    }

    /**
     * @return how long what was drawn stays true: a second while the clock shows seconds, a minute
     * for the bar alone once it counts in days.
     */
    public long refreshMillis(@Nonnull Instant now) {
        return secondsLeft(now) < DAY_SECONDS ? 1_000 : 60_000;
    }
}
