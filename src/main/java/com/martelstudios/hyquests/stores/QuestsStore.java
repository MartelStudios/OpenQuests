package com.martelstudios.hyquests.stores;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.datastore.DataStore;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Persists a {@link QuestsRecord} per named scope. Holding the record here rather than in the
 * caller keeps a single source of truth: mutating what {@link #get} returns is what gets saved.
 */
public class QuestsStore {
    @Nonnull
    private final static HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Nonnull
    private final Map<String, QuestsRecord> quests = new ConcurrentHashMap<>();

    @Nonnull
    public final DataStore<QuestsRecord> dataStore;

    public QuestsStore(@Nonnull DataStore<QuestsRecord> dataStore) {
        this.dataStore = dataStore;
    }

    /**
     * @return the live record of a scope, empty if it has none yet. Never {@code null}.
     */
    @Nonnull
    public QuestsRecord get(@Nonnull String scope) {
        return quests.computeIfAbsent(scope, _ -> new QuestsRecord());
    }

    /**
     * Reads a scope back from disk. A missing file is the normal first-run case and yields an
     * empty record, since a {@code ConcurrentHashMap} would reject the null.
     */
    @Nonnull
    public QuestsRecord load(@Nonnull String scope) {
        try {
            QuestsRecord record = dataStore.load(scope);
            if (record != null) quests.put(scope, record);
        } catch (IOException e) {
            LOGGER.atWarning().withCause(e).log("Failed to load %s quests from disk", scope);
        }

        return get(scope);
    }

    public void save(@Nonnull String scope) {
        QuestsRecord record = quests.get(scope);
        if (record == null) return;

        dataStore.save(scope, record);
    }
}
