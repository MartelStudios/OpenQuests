package com.martelstudios.openquests.core.replication;

import com.martelstudios.openquests.core.models.QuestState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;

/**
 * Where a quest stands as the storage holds it, which every server holding the quest takes in.
 *
 * @param at when it reached that state if it is an outcome, {@code null} while it runs
 * @param epoch how many changes of state led there: a copy only ever takes in a later one
 */
public record StoredState(@Nonnull QuestState state, @Nullable Instant at, long epoch) {}
