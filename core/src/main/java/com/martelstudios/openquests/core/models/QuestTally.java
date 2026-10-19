package com.martelstudios.openquests.core.models;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.util.Collection;
import java.util.UUID;

/**
 * Where one player stands with one asset: the quests from it that ended for them, as their record
 * counts them, and those still running. Built when asked, never stored.
 *
 * <p>A quest that ended is counted by the record alone, a running one by the state it has for the
 * player, so none is counted twice: the record takes a quest in at the very moment it ends.
 */
public final class QuestTally {

    /**
     * A player who never held a quest from the asset.
     */
    public static final QuestTally NONE = new QuestTally(QuestCompletions.NONE, 0, 0, null);

    @Nonnull
    private final QuestCompletions completions;

    private final int running;

    /**
     * Shared quests this player gave up while the others carry on: the record only hears of them as
     * the quest ends, yet for this player they are abandoned already.
     */
    private final int leftRunning;

    @Nullable
    private final Instant lastRunningStartedAt;

    private QuestTally(@Nonnull QuestCompletions completions, int running, int leftRunning, @Nullable Instant lastRunningStartedAt) {
        this.completions = completions;
        this.running = running;
        this.leftRunning = leftRunning;
        this.lastRunningStartedAt = lastRunningStartedAt;
    }

    /**
     * @param completions how the player's quests from the asset ended so far
     * @param quests the quests from the asset in memory that the player holds or gave up, ended or
     *               not; those that ended are left to the record
     */
    @Nonnull
    public static QuestTally of(@Nonnull QuestCompletions completions, @Nonnull UUID playerId, @Nonnull Collection<? extends AbstractQuestProgression<?>> quests) {
        int running = 0;
        int leftRunning = 0;
        Instant lastStartedAt = null;

        for (AbstractQuestProgression<?> quest : quests) {
            if (quest.isCompleted()) continue;

            if (quest.isAbandonedBy(playerId)) {
                leftRunning++;
            } else if (quest.getPlayers().contains(playerId)) {
                running++;
            } else {
                continue;
            }

            Instant startedAt = quest.getStartedAt();
            if (startedAt != null && (lastStartedAt == null || startedAt.isAfter(lastStartedAt))) lastStartedAt = startedAt;
        }

        if (running == 0 && leftRunning == 0 && completions == QuestCompletions.NONE) return NONE;
        return new QuestTally(completions, running, leftRunning, lastStartedAt);
    }

    /**
     * @return how many of the player's quests from the asset stand in that state.
     */
    public int count(@Nonnull QuestState state) {
        return switch (state) {
            case IN_PROGRESS -> running;
            case ABANDONED -> completions.count(QuestState.ABANDONED) + leftRunning;
            default -> completions.count(state);
        };
    }

    /**
     * @return how many of the player's quests from the asset stand in any of those states.
     */
    public int count(@Nonnull Collection<QuestState> states) {
        int total = 0;
        for (QuestState state : states) total += count(state);
        return total;
    }

    /**
     * @return when the last of the player's quests from the asset started, running ones included,
     * {@code null} if none said.
     */
    @Nullable
    public Instant getLastStartedAt() {
        Instant recorded = completions.getLastStartedAt();
        if (recorded == null) return lastRunningStartedAt;
        if (lastRunningStartedAt == null) return recorded;

        return lastRunningStartedAt.isAfter(recorded) ? lastRunningStartedAt : recorded;
    }

    /**
     * @return when the last of the player's quests from the asset ended, whichever way, {@code null}
     * if none has.
     */
    @Nullable
    public Instant getLastCompletedAt() {
        return completions.getLastCompletedAt();
    }
}
