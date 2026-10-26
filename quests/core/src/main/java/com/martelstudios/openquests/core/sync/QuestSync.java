package com.martelstudios.openquests.core.sync;

import com.martelstudios.openquests.core.models.AbstractQuestProgression;

import javax.annotation.Nonnull;

/**
 * What a quest asks about the other servers that may write it, so the model knows nothing of where
 * quests are stored: whether its changes are to be made again over theirs, and when to write them.
 */
public interface QuestSync {

    /**
     * @return whether other servers may write that quest too, which makes each change to it one
     * to make again over what they stored meanwhile.
     */
    boolean isShared(@Nonnull AbstractQuestProgression<?> quest);

    /**
     * Writes what the quest is waiting to write off the caller's thread, the caller never waiting
     * on the storage.
     */
    void writeSoon(@Nonnull AbstractQuestProgression<?> quest);
}
