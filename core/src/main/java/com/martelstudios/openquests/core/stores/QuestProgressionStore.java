package com.martelstudios.openquests.core.stores;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.datastore.DataStore;
import com.martelstudios.openquests.core.assets.QuestAsset;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class QuestProgressionStore {
    @Nonnull
    private final static HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Nonnull
    private final Map<UUID, AbstractQuestProgression<?>> quests = new ConcurrentHashMap<>();

    @Nonnull
    private final Map<Class<?>, Set<UUID>> idsByType = new ConcurrentHashMap<>();

    @Nonnull
    public final DataStore<QuestProgressionRecord> dataStore;

    public QuestProgressionStore(@Nonnull DataStore<QuestProgressionRecord> dataStore) {
        this.dataStore = dataStore;
    }

    public void add(@Nonnull AbstractQuestProgression<?> quest) {
        if (quests.containsKey(quest.getId())) return;

        quests.put(quest.getId(), quest);
        idsByType.computeIfAbsent(quest.getClass(), k -> ConcurrentHashMap.newKeySet()).add(quest.getId());
    }

    public AbstractQuestProgression<?> remove(@Nonnull UUID id) {
        if (!quests.containsKey(id)) return null;

        AbstractQuestProgression<?> quest = quests.remove(id);

        Set<UUID> ids = idsByType.get(quest.getClass());
        if (ids != null) ids.remove(id);

        return quest;
    }

    /**
     * Removes a quest from memory and deletes its persisted file, if any.
     */
    public AbstractQuestProgression<?> removeAndDeleteFromDisk(@Nonnull UUID id) {
        AbstractQuestProgression<?> quest = remove(id);
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
    public AbstractQuestProgression<?> get(@Nonnull UUID id) {
        AbstractQuestProgression<?> quest = quests.get(id);
        return quest != null ? quest : load(id);
    }

    @Nonnull
    public Collection<AbstractQuestProgression<?>> getAll() {
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
     * Persists a single quest, but only if {@link AbstractQuestProgression#hasChanges()} and {@link QuestAsset#isPersistProgression()}
     */
    public void saveToDisk(@Nonnull AbstractQuestProgression<?> quest) {
        if (!quest.consumeChanges()) return;

        QuestAsset asset = quest.getAsset();
        if (asset != null && !asset.isPersistProgression()) return;

        dataStore.save(quest.getId().toString(), new QuestProgressionRecord(quest));
    }

    /**
     * Persists every quest that changed since the last pass.
     */
    public void saveAllToDisk() {
        for (AbstractQuestProgression<?> quest : quests.values()) {
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

    public AbstractQuestProgression<?> load(UUID id) {
        AbstractQuestProgression<?> quest = quests.get(id);
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
