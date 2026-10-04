package com.martelstudios.openquests.core.assignments;

import com.martelstudios.openquests.core.models.AssignmentRecord;
import com.martelstudios.openquests.core.models.AssignmentRecords;
import com.martelstudios.openquests.core.persistence.QuestStorage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The records of the holders many players share, read from the store once and kept. A copy gone
 * stale is harmless: every write is conditional on what was read, so a stale copy can only lose a
 * claim, never hand an occasion out twice, and a lost claim reads the records again.
 */
public final class SharedAssignmentRecords {

    @Nonnull
    private final QuestStorage storage;

    private final Map<String, AssignmentRecords> byHolder = new ConcurrentHashMap<>();

    public SharedAssignmentRecords(@Nonnull QuestStorage storage) {
        this.storage = storage;
    }

    /**
     * @return the records of that holder, read from the store the first time only.
     */
    @Nonnull
    public AssignmentRecords get(@Nonnull String holderKey) {
        AssignmentRecords records = byHolder.get(holderKey);
        if (records != null) return records;

        // Read outside the map's lock, which a slow store would otherwise hold for every holder
        AssignmentRecords read = storage.loadAssignments(holderKey);
        AssignmentRecords raced = byHolder.putIfAbsent(holderKey, read);
        return raced != null ? raced : read;
    }

    /**
     * Writes the next record if the stored one is still the one read, and keeps it on success.
     * A claim another server won leaves the copy read again instead.
     *
     * @return whether the write went through.
     */
    public boolean claim(@Nonnull String holderKey, @Nonnull String assignmentId, @Nonnull String questAssetId, @Nullable AssignmentRecord expected, @Nonnull AssignmentRecord next) {
        if (!storage.claimAssignment(holderKey, assignmentId, questAssetId, expected, next)) {
            reload(holderKey);
            return false;
        }

        get(holderKey).put(assignmentId, questAssetId, next);
        return true;
    }

    /**
     * Reads the records of that holder again, for one another server wrote to since.
     */
    public void reload(@Nonnull String holderKey) {
        byHolder.put(holderKey, storage.loadAssignments(holderKey));
    }

    /**
     * Drops a holder gone for good, a world closing, whose records are deleted with it.
     */
    public void forget(@Nonnull String holderKey) {
        byHolder.remove(holderKey);
    }
}
