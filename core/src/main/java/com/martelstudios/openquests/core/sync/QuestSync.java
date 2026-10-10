package com.martelstudios.openquests.core.sync;

import com.martelstudios.openquests.core.models.AbstractQuestProgression;

import javax.annotation.Nonnull;

/**
 * What a quest asks about the other servers that may write it, so the model knows nothing of where
 * quests are stored: whether its changes are to be made again over theirs, and when to write them.
 */
public final class QuestSync {

    /**
     * Answers for the storage the quests are kept in.
     */
    public interface Policy {

        /**
         * @return whether other servers may write that quest too, which makes each change to it one
         * to make again over what they stored meanwhile.
         */
        boolean isShared(@Nonnull AbstractQuestProgression<?> quest);

        /**
         * Writes what the quest is waiting to write off the caller's thread, the caller never
         * waiting on the storage.
         */
        void writeSoon(@Nonnull AbstractQuestProgression<?> quest);
    }

    /**
     * A server alone writes its quests over themselves: nothing to make again.
     */
    private static final Policy ALONE = new Policy() {
        @Override
        public boolean isShared(@Nonnull AbstractQuestProgression<?> quest) {
            return false;
        }

        @Override
        public void writeSoon(@Nonnull AbstractQuestProgression<?> quest) {}
    };

    private static volatile Policy policy = ALONE;

    private QuestSync() {}

    /**
     * Set once as the storage starts.
     */
    public static void setPolicy(@Nonnull Policy value) {
        policy = value;
    }

    /**
     * @return whether other servers may write that quest too.
     */
    public static boolean isShared(@Nonnull AbstractQuestProgression<?> quest) {
        return policy.isShared(quest);
    }

    /**
     * Writes what the quest is waiting to write, off the caller's thread.
     */
    public static void writeSoon(@Nonnull AbstractQuestProgression<?> quest) {
        policy.writeSoon(quest);
    }
}
