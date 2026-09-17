package com.martelstudios.openquests.core.stores;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.datastore.DataStore;
import com.martelstudios.openquests.core.events.QuestLoadedEvent;
import com.martelstudios.openquests.core.events.QuestUnloadedEvent;
import com.martelstudios.openquests.core.models.QuestAsset;
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

    /**
     * Quests that ended and are kept for the reading. Held apart from the live ones rather than
     * told apart by their state: everything that walks the store — a ticking system, a visitor —
     * wants what can still move, and a quest that is over would be walked forever otherwise.
     *
     * <p>Reachable by id all the same, which is what lets a finished chain still be opened, its
     * steps still be named, and a prerequisite still be answered.
     */
    @Nonnull
    private final Map<UUID, AbstractQuestProgression<?>> archived = new ConcurrentHashMap<>();

    @Nonnull
    private final Map<Class<?>, Set<UUID>> idsByType = new ConcurrentHashMap<>();

    /**
     * Quests that have a file of their own. A quest earns one by being shared, and keeps it even if
     * it later drops back to a single player.
     */
    @Nonnull
    private final Set<UUID> fileBacked = ConcurrentHashMap.newKeySet();

    @Nonnull
    public final DataStore<QuestProgressionRecord> dataStore;

    public QuestProgressionStore(@Nonnull DataStore<QuestProgressionRecord> dataStore) {
        this.dataStore = dataStore;
    }

    /**
     * A quest read back from disk having already ended goes straight to the archive, so the split
     * survives a restart without being written down anywhere.
     */
    public void add(@Nonnull AbstractQuestProgression<?> quest) {
        if (quests.containsKey(quest.getId()) || archived.containsKey(quest.getId())) return;

        if (quest.isCompleted() && quest.isStopOnComplete()) {
            archived.put(quest.getId(), quest);
        } else {
            quests.put(quest.getId(), quest);
            idsByType.computeIfAbsent(quest.getClass(), k -> ConcurrentHashMap.newKeySet()).add(quest.getId());
        }

        HytaleServer.get()
                    .getEventBus()
                    .dispatchFor(QuestLoadedEvent.class, quest.getId())
                    .dispatch(new QuestLoadedEvent(quest));
    }

    /**
     * Drops a quest from memory, leaving its file where it is: the next lookup by id reads it back.
     * What it changed is written out first, since nothing else will now hold it.
     *
     * @return {@code null} for a quest the store was not holding.
     */
    public AbstractQuestProgression<?> unload(@Nonnull UUID id) {
        AbstractQuestProgression<?> quest = quests.containsKey(id) ? quests.get(id) : archived.get(id);
        if (quest == null) return null;

        saveToDisk(quest);

        if (remove(id) == null) archived.remove(id);

        HytaleServer.get()
                    .getEventBus()
                    .dispatchFor(QuestUnloadedEvent.class, id)
                    .dispatch(new QuestUnloadedEvent(quest));

        return quest;
    }

    /**
     * Moves a quest that ended out of the way of everything that walks the live ones, keeping it
     * answerable by id. Its file stays where it is: what changed is which map holds it, and the
     * quest itself already says it is over.
     *
     * @return {@code false} for a quest the live half was not holding.
     */
    public boolean archive(@Nonnull AbstractQuestProgression<?> quest) {
        if (remove(quest.getId()) == null) return false;

        archived.put(quest.getId(), quest);
        return true;
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
        if (quest == null) quest = archived.remove(id);
        if (quest == null) return null;

        fileBacked.remove(id);

        try {
            dataStore.remove(id.toString());
        } catch (IOException e) {
            LOGGER.atWarning().withCause(e).log("Failed to delete quest %s from disk", id);
        }

        return quest;
    }

    /**
     * @return the quest under that id, running or ended alike, or {@code null} for one that is not
     * in memory. Looking at a quest never reads it back in: {@link #load} is the only way in, so
     * that a store nobody is filling can only shrink.
     */
    @Nullable
    public AbstractQuestProgression<?> get(@Nonnull UUID id) {
        AbstractQuestProgression<?> quest = quests.get(id);

        return quest != null ? quest : archived.get(id);
    }

    /**
     * @return the quest under that id only if it is still running, so a caller meaning to progress
     * something never lands on one that is over.
     */
    @Nullable
    public AbstractQuestProgression<?> getLive(@Nonnull UUID id) {
        return quests.get(id);
    }

    /**
     * @return every quest still running. What ended is left out: a caller walking the store is
     * looking for something to do, and the archive is only ever read by name.
     */
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
        if (!isFileBacked(quest)) return;

        dataStore.save(quest.getId().toString(), new QuestProgressionRecord(quest));
        fileBacked.add(quest.getId());
    }

    /**
     * A quest held by a single player is written with that player instead, which is what keeps the
     * quest directory from growing with one file per player and per quest.
     */
    public boolean isFileBacked(@Nonnull AbstractQuestProgression<?> quest) {
        return quest.getHolderCount() > 1 || fileBacked.contains(quest.getId());
    }

    /**
     * Persists every quest that changed since the last pass.
     */
    public void saveAllToDisk() {
        for (AbstractQuestProgression<?> quest : quests.values()) {
            saveToDisk(quest);
        }

        // The archive too: a quest is marked dirty by the very change that ended it, and skipping
        // it here would leave the outcome it was archived for unwritten
        for (AbstractQuestProgression<?> quest : archived.values()) {
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

    /**
     * Reads a quest back in, or hands over the one already there. The way into the store, and the
     * one place a lookup is allowed to cost a disk read and to announce what it found.
     *
     * @return {@code null} for an id nothing on disk answers to.
     */
    @Nullable
    public AbstractQuestProgression<?> load(@Nonnull UUID id) {
        AbstractQuestProgression<?> quest = quests.get(id);
        if (quest == null) quest = archived.get(id);
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
        fileBacked.add(id);

        // Whichever half add() put it in: one that ended came back to the archive
        return record.quest;
    }

}
