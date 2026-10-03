package com.martelstudios.openquests.core.assignments;

import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.AssignmentRecord;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;

/**
 * Whoever an assignment hands a quest to: one player, or a world, a group of worlds or the server
 * shared by many. The assignment decides the same way for all; only where the records live and how
 * a quest reaches them differ.
 */
public interface AssignmentHolder {

    /**
     * @return the holder's key, the same as its index: {@code player:<uuid>},
     * {@code world:<uuid>}, {@code universe}.
     */
    @Nonnull
    String getKey();

    /**
     * @return what that assignment already handed this holder of that quest, {@code null} if
     * nothing yet.
     */
    @Nullable
    AssignmentRecord getRecord(@Nonnull String assignmentId, @Nonnull String questAssetId);

    /**
     * @return the quests this holder takes part in, those that ended included.
     */
    @Nonnull
    Collection<AbstractQuestProgression<?>> getQuests();

    /**
     * @return whether the quest is still running as far as this holder goes: a player who gave it
     * up is done with it, whatever the others do.
     */
    boolean isRunning(@Nonnull AbstractQuestProgression<?> quest);

    /**
     * Hands a new quest out and writes the record down, both or neither.
     *
     * @param expected the record read before deciding, which must still be the one stored
     * @return {@code false} if the quest was refused or another server handed this occasion out first.
     */
    boolean handOut(@Nonnull AbstractQuestProgression<?> quest, @Nonnull String assignmentId, @Nonnull String questAssetId, @Nullable AssignmentRecord expected, @Nonnull AssignmentRecord next);
}
