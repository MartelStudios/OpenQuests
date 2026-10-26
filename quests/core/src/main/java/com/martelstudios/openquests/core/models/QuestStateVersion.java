package com.martelstudios.openquests.core.models;

import javax.annotation.Nonnull;

/**
 * Where a quest stands and how many changes of state brought it there: the version orders news of
 * one quest heard out of order, a state alone repeating itself for a quest kept running.
 *
 * @param version {@code 0} for a quest that never changed state
 */
public record QuestStateVersion(@Nonnull QuestState state, int version) {

    public static final QuestStateVersion INITIAL = new QuestStateVersion(QuestState.IN_PROGRESS, 0);

    /**
     * @return this one moved to that state, one version later, or this one if it stands there already.
     */
    @Nonnull
    public QuestStateVersion with(@Nonnull QuestState next) {
        return next == state ? this : new QuestStateVersion(next, version + 1);
    }

    /**
     * @return whether this is later news than that version.
     */
    public boolean isNewerThan(int known) {
        return version > known;
    }
}
