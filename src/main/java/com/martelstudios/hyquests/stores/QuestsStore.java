package com.martelstudios.hyquests.stores;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.datastore.DataStore;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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

    public QuestsRecord add(String scope, UUID questId) {
        quests.computeIfAbsent(scope, _ -> new QuestsRecord()).register(questId);
        return quests.get(scope);
    }

    public QuestsRecord remove(String scope, UUID questId) {
        quests.computeIfPresent(scope, (_, record) -> {
            record.unregister(questId);
            return record;
        });
        return quests.get(scope);
    }

    public QuestsRecord load(String scope) {
        try {
            quests.put(scope, dataStore.load(scope));
        } catch (IOException e) {
            LOGGER.atWarning().withCause(e).log("Failed to load %s quests from disk", scope);
        }

        return quests.get(scope);
    }

    public Map<String, QuestsRecord> loadAll() {
        try {
            quests.putAll(dataStore.loadAll());
        } catch (IOException e) {
            LOGGER.atWarning().withCause(e).log("Failed to load quests from disk");
        }

        return quests;
    }

    public void save(String scope) {
        dataStore.save(scope, quests.get(scope));
    }

    public void saveAll() {
        quests.forEach(dataStore::save);
    }
}
