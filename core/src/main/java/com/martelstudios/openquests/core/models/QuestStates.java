package com.martelstudios.openquests.core.models;

import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;

import javax.annotation.Nonnull;

/**
 * A list of quest states as assets write it, {@code "States": ["InProgress", "Successful"]}, for
 * every rule that counts or asks for quests standing in some of them.
 */
public final class QuestStates {

    @Nonnull
    public static final ArrayCodec<QuestState> CODEC = new ArrayCodec<>(new EnumCodec<>(QuestState.class), QuestState[]::new);

    /**
     * Every state, running and ended alike.
     */
    @Nonnull
    public static final QuestState[] ALL = QuestState.values();

    /**
     * Every outcome, however the quest ended.
     */
    @Nonnull
    public static final QuestState[] ENDED = {QuestState.SUCCESSFUL, QuestState.FAILED, QuestState.ABANDONED};

    private QuestStates() {}
}
