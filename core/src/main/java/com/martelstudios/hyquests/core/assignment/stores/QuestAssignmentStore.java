package com.martelstudios.hyquests.core.assignment.stores;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.datastore.DataStore;
import com.martelstudios.hyquests.core.assignment.models.QuestAssignment;
import com.martelstudios.hyquests.core.stores.QuestProgressionStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Every assignment instance, whoever it is offered to. Mirrors {@link QuestProgressionStore}:
 * one file per assignment, with {@link QuestAssignmentStoreComponent} as the per-player index.
 */
public class QuestAssignmentStore {
    @Nonnull
    private final static HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Nonnull
    private final Map<UUID, QuestAssignment> assignments = new ConcurrentHashMap<>();

    @Nonnull
    public final DataStore<QuestAssignmentRecord> dataStore;

    public QuestAssignmentStore(@Nonnull DataStore<QuestAssignmentRecord> dataStore) {
        this.dataStore = dataStore;
    }

    public void add(@Nonnull QuestAssignment assignment) {
        assignments.putIfAbsent(assignment.getId(), assignment);
    }

    public QuestAssignment removeAndDeleteFromDisk(@Nonnull UUID id) {
        QuestAssignment assignment = assignments.remove(id);
        if (assignment == null) return null;

        try {
            dataStore.remove(id.toString());
        } catch (IOException e) {
            LOGGER.atWarning().withCause(e).log("Failed to delete quest assignment %s from disk", id);
        }

        return assignment;
    }

    /**
     * Falls back to disk on a miss, so callers never have to care whether it was loaded yet.
     */
    @Nullable
    public QuestAssignment get(@Nonnull UUID id) {
        QuestAssignment assignment = assignments.get(id);
        return assignment != null ? assignment : load(id);
    }

    @Nonnull
    public Collection<QuestAssignment> getAll() {
        return assignments.values();
    }

    public QuestAssignment load(@Nonnull UUID id) {
        QuestAssignment assignment = assignments.get(id);
        if (assignment != null) return assignment;

        QuestAssignmentRecord record;
        try {
            record = dataStore.load(id.toString());
        } catch (IOException e) {
            LOGGER.atWarning().withCause(e).log("Failed to load quest assignment %s from disk", id);
            return null;
        }

        if (record == null || record.assignment == null) return null;

        add(record.assignment);
        return assignments.get(id);
    }

    public void saveAllToDisk() {
        for (QuestAssignment assignment : assignments.values()) {
            if (!assignment.consumeChanges()) continue;

            dataStore.save(assignment.getId().toString(), new QuestAssignmentRecord(assignment));
        }
    }
}
