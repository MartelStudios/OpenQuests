package com.martelstudios.hyquests.core.stores;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.datastore.DataStore;
import com.martelstudios.hyquests.core.models.AbstractQuest;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class QuestProgressionStore {
    @Nonnull
    private final static HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Nonnull
    private final Map<UUID, AbstractQuest<?>> quests = new ConcurrentHashMap<>();

    @Nonnull
    private final Map<Class<?>, Set<UUID>> idsByType = new ConcurrentHashMap<>();

    @Nonnull
    public final DataStore<QuestProgressionRecord> dataStore;

    public QuestProgressionStore(@Nonnull DataStore<QuestProgressionRecord> dataStore) {
        this.dataStore = dataStore;
    }

    public void add(@Nonnull AbstractQuest<?> quest) {
        if (quests.containsKey(quest.getId())) return;

        quests.put(quest.getId(), quest);
        idsByType.computeIfAbsent(quest.getClass(), k -> ConcurrentHashMap.newKeySet()).add(quest.getId());
    }

    public AbstractQuest<?> remove(@Nonnull UUID id) {
        if (!quests.containsKey(id)) return null;

        AbstractQuest<?> quest = quests.remove(id);

        Set<UUID> ids = idsByType.get(quest.getClass());
        if (ids != null) ids.remove(id);

        return quest;
    }

    /**
     * Removes a quest from memory and deletes its persisted file, if any.
     */
    public AbstractQuest<?> removeAndDeleteFromDisk(@Nonnull UUID id) {
        AbstractQuest<?> quest = remove(id);
        if (quest == null) return null;

        try {
            dataStore.remove(id.toString());
        } catch (IOException e) {
            LOGGER.atWarning().withCause(e).log("Failed to delete quest %s from disk", id);
        }

        return quest;
    }

    /**
     * Falls back to disk on a miss, so callers never have to care whether a quest was loaded
     * yet. A genuinely deleted quest costs one {@code Files.exists} check and returns {@code null}.
     */
    @Nullable
    public AbstractQuest<?> get(@Nonnull UUID id) {
        AbstractQuest<?> quest = quests.get(id);
        return quest != null ? quest : load(id);
    }

    @Nonnull
    public Collection<AbstractQuest<?>> getAll() {
        return quests.values();
    }

    /**
     * @return the ids of every registered quest of the given concrete type, or an
     * empty set if none. Never {@code null}.
     */
    @Nonnull
    public Set<UUID> getForType(@Nonnull Class<?> questClass) {
        Set<UUID> ids = idsByType.get(questClass);
        return ids != null ? ids : Collections.emptySet();
    }

    /**
     * Persists a single quest, but only if it changed since the last save.
     */
    public void saveToDisk(@Nonnull AbstractQuest<?> quest) {
        if (!quest.consumeChanges()) return;

        dataStore.save(quest.getId().toString(), new QuestProgressionRecord(quest));
    }

    /**
     * Persists every quest that changed since the last pass.
     */
    public void saveAllToDisk() {
        for (AbstractQuest<?> quest : quests.values()) {
            saveToDisk(quest);
        }
    }

    /**
     * Loads every persisted quest from disk into memory, rebuilding the type index.
     */
    public void loadAllFromDisk() {
        Map<String, QuestProgressionRecord> records;
        try {
            records = dataStore.loadAll();
        } catch (IOException e) {
            LOGGER.atWarning().withCause(e).log("Failed to load quests from disk");
            return;
        }

        for (QuestProgressionRecord record : records.values()) {
            if (record == null || record.quest == null) continue;
            add(record.quest);
        }
    }

    public AbstractQuest<?> load(UUID id) {
        AbstractQuest<?> quest = quests.get(id);
        if (quest != null) return quest;

        QuestProgressionRecord record;
        try {
            record = dataStore.load(id.toString());
        } catch (IOException e) {
            LOGGER.atWarning().withCause(e).log("Failed to load quest %s from disk", id);
            return null;
        }

        if (record == null || record.quest == null) return null;

        add(record.quest);
        return quests.get(id);
    }

}
