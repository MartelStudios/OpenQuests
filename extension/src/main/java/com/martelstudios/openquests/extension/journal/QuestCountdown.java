package com.martelstudios.openquests.extension.journal;

import com.hypixel.hytale.server.core.Message;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;

/**
 * A figure the journal keeps counting down while it stays open: the time left on a quest, or the
 * wait before it can be taken again. The page redraws it, since only the page knows it is still on
 * screen.
 *
 * @param until   the moment it reaches zero
 * @param wrapKey a translation taking the duration as {@code duration}, or {@code null} for the
 *                bare duration
 */
public record QuestCountdown(@Nonnull Instant until, @Nullable String wrapKey) {

    @Nonnull
    public Message format(@Nonnull Instant now) {
        Message duration = QuestPageRows.formatDuration(Duration.between(now, until));

        return wrapKey == null ? duration : Message.translation(wrapKey).param("duration", duration);
    }

    public boolean isOver(@Nonnull Instant now) {
        return !now.isBefore(until);
    }
}
