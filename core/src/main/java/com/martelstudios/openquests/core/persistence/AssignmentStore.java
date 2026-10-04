package com.martelstudios.openquests.core.persistence;

import com.martelstudios.openquests.core.models.AssignmentRecord;
import com.martelstudios.openquests.core.models.AssignmentRecords;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * What the assignments handed the holders many players share: a world, a group of worlds, the
 * server. A player's own are part of their record.
 */
public interface AssignmentStore {

    /**
     * @return the records of that holder, empty for one handed nothing yet.
     */
    @Nonnull
    AssignmentRecords loadAssignments(@Nonnull String holderKey);

    /**
     * Writes one record only if what is stored still matches what the caller read, so servers
     * sharing a holder never both hand the same occasion out.
     *
     * @param expected what the caller read, {@code null} when there was nothing
     * @return {@code false} if something else wrote in between, nothing then written.
     */
    boolean claimAssignment(@Nonnull String holderKey, @Nonnull String assignmentId, @Nonnull String questAssetId, @Nullable AssignmentRecord expected, @Nonnull AssignmentRecord next);

    /**
     * Lets go of a holder nothing will hand anything to again, such as a closed world.
     */
    void deleteAssignments(@Nonnull String holderKey);
}
