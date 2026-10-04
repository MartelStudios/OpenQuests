package com.martelstudios.openquests.core.replication;

import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.QuestState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Decides who ends a quest several servers hold: the first to claim it. Kept apart from the
 * quest, which only asks, so the model knows nothing of where quests are stored.
 */
public final class QuestEnds {

    /**
     * Claims a quest's first end for this server.
     */
    @FunctionalInterface
    public interface Arbiter {

        /**
         * @return {@code null} if this server ended the quest, with the outcome it carries; the
         * outcome another server claimed first otherwise.
         */
        @Nullable
        QuestState claim(@Nonnull AbstractQuestProgression<?> quest);
    }

    /**
     * A server alone ends every quest it sees end.
     */
    private static volatile Arbiter arbiter = quest -> null;

    private QuestEnds() {}

    /**
     * Set once as the storage starts.
     */
    public static void setArbiter(@Nonnull Arbiter value) {
        arbiter = value;
    }

    /**
     * @return {@code null} if this server ended the quest; the outcome another server claimed first
     * otherwise, which the quest takes instead of its own.
     */
    @Nullable
    public static QuestState claim(@Nonnull AbstractQuestProgression<?> quest) {
        return arbiter.claim(quest);
    }
}
