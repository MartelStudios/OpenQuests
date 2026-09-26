package com.martelstudios.openquests.core.models;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;

/**
 * What one player has done with one asset, counted as each of its quests ends. Kept apart from the
 * quests themselves, so it outlives an asset that keeps no history.
 *
 * <p>Never changed once built: counting an outcome returns a new instance, so one thread can read
 * it while another replaces it.
 */
public final class QuestCompletions {

    /**
     * An asset the player never finished a quest from.
     */
    public static final QuestCompletions NONE = new QuestCompletions();

    /**
     * A count left at zero and a date never set are left out, so an asset done once costs one line.
     */
    public static final BuilderCodec<QuestCompletions> CODEC = BuilderCodec.builder(QuestCompletions.class, QuestCompletions::new)
                                                                           .append(new KeyedCodec<>("Successful", Codec.INTEGER), (completions, count) -> completions.successful = count, completions -> completions.successful == 0 ? null : Integer.valueOf(completions.successful))
                                                                           .add()
                                                                           .append(new KeyedCodec<>("Failed", Codec.INTEGER), (completions, count) -> completions.failed = count, completions -> completions.failed == 0 ? null : Integer.valueOf(completions.failed))
                                                                           .add()
                                                                           .append(new KeyedCodec<>("Abandoned", Codec.INTEGER), (completions, count) -> completions.abandoned = count, completions -> completions.abandoned == 0 ? null : Integer.valueOf(completions.abandoned))
                                                                           .add()
                                                                           .append(new KeyedCodec<>("LastStartedAt", Codec.LONG), (completions, millis) -> completions.lastStartedAt = Instant.ofEpochMilli(millis), completions -> completions.lastStartedAt == null ? null : Long.valueOf(completions.lastStartedAt.toEpochMilli()))
                                                                           .add()
                                                                           .append(new KeyedCodec<>("LastCompletedAt", Codec.LONG), (completions, millis) -> completions.lastCompletedAt = Instant.ofEpochMilli(millis), completions -> completions.lastCompletedAt == null ? null : Long.valueOf(completions.lastCompletedAt.toEpochMilli()))
                                                                           .add()
                                                                           .build();

    private int successful;
    private int failed;
    private int abandoned;

    @Nullable
    private Instant lastStartedAt;

    @Nullable
    private Instant lastCompletedAt;

    private QuestCompletions() {}

    private QuestCompletions(@Nonnull QuestCompletions other) {
        this.successful = other.successful;
        this.failed = other.failed;
        this.abandoned = other.abandoned;
        this.lastStartedAt = other.lastStartedAt;
        this.lastCompletedAt = other.lastCompletedAt;
    }

    /**
     * Counts one more quest ended that way. The dates only move forward, so an older quest heard of
     * late cannot pass for the latest one.
     *
     * @param startedAt when that quest started, {@code null} if it never said
     * @param completedAt when it ended, {@code null} if it never said
     * @return the new count, or this one unchanged for a state that is not an outcome.
     */
    @Nonnull
    public QuestCompletions record(@Nonnull QuestState outcome, @Nullable Instant startedAt, @Nullable Instant completedAt) {
        if (outcome == QuestState.IN_PROGRESS) return this;

        QuestCompletions next = new QuestCompletions(this);
        switch (outcome) {
            case SUCCESSFUL -> next.successful++;
            case FAILED -> next.failed++;
            case ABANDONED -> next.abandoned++;
            default -> {}
        }

        next.lastStartedAt = latest(lastStartedAt, startedAt);
        next.lastCompletedAt = latest(lastCompletedAt, completedAt);
        return next;
    }

    /**
     * @return how many quests from the asset ended that way for the player, zero for a state that is
     * not an outcome.
     */
    public int count(@Nonnull QuestState outcome) {
        return switch (outcome) {
            case SUCCESSFUL -> successful;
            case FAILED -> failed;
            case ABANDONED -> abandoned;
            default -> 0;
        };
    }

    /**
     * @return how many quests from the asset ended at all, whichever way.
     */
    public int total() {
        return successful + failed + abandoned;
    }

    /**
     * @return when the last quest counted here started, {@code null} if none said. Dates are the
     * quest's own, so a player who joined a shared quest late reads when the quest began.
     */
    @Nullable
    public Instant getLastStartedAt() {
        return lastStartedAt;
    }

    /**
     * @return when the last quest counted here ended, whichever way, {@code null} if none said.
     */
    @Nullable
    public Instant getLastCompletedAt() {
        return lastCompletedAt;
    }

    @Nullable
    private static Instant latest(@Nullable Instant current, @Nullable Instant candidate) {
        if (current == null) return candidate;
        if (candidate == null) return current;

        return candidate.isAfter(current) ? candidate : current;
    }
}
