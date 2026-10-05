package com.martelstudios.openquests.core.replication;

import com.martelstudios.openquests.core.models.AbstractQuestProgression;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Decides which change of state a quest goes through when several servers hold it: the first to
 * claim it from where the quest stood. Kept apart from the quest, which only asks, so the model
 * knows nothing of where quests are stored.
 */
public final class QuestTransitions {

    /**
     * Claims the change of state a quest just went through for this server.
     */
    @FunctionalInterface
    public interface Arbiter {

        /**
         * @param quest carrying its new state, and still the epoch it changed from
         * @return {@code null} if the change is this server's to make; otherwise where another
         * server moved the quest first, which the quest takes instead.
         */
        @Nullable
        StoredState claim(@Nonnull AbstractQuestProgression<?> quest);
    }

    /**
     * A server alone makes every change it sees.
     */
    private static volatile Arbiter arbiter = quest -> null;

    private QuestTransitions() {}

    /**
     * Set once as the storage starts.
     */
    public static void setArbiter(@Nonnull Arbiter value) {
        arbiter = value;
    }

    /**
     * @return {@code null} if this server makes the change; where the quest stands otherwise.
     */
    @Nullable
    public static StoredState claim(@Nonnull AbstractQuestProgression<?> quest) {
        return arbiter.claim(quest);
    }
}
