package com.martelstudios.openquests.core.scopes;

import com.martelstudios.openquests.core.OpenQuestsCorePlugin;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.persistence.IndexStore;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.core.stores.QuestsRecord;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The running quests of every scope sharing them, one index per key: the universe, each world,
 * each group of worlds. An index is read back the first time anything asks for it and written as
 * it changes, member by member, so a scope only ever says what it adds and what it takes off.
 *
 * <p>Thread safe: an index is read and written from the game threads and the storage thread alike.
 */
public class ScopeIndexes {
    @Nonnull
    private final IndexStore storage;

    @Nonnull
    private final Writer writer;

    private final Map<String, QuestsRecord> records = new ConcurrentHashMap<>();

    /**
     * @param writer where each change goes once made, so the game threads never wait on the storage
     */
    public ScopeIndexes(@Nonnull IndexStore storage, @Nonnull Writer writer) {
        this.storage = storage;
        this.writer = writer;
    }

    public static ScopeIndexes get() {
        return OpenQuestsCorePlugin.get().getScopeIndexes();
    }

    /**
     * @return the live ids under that key, read back first if nothing asked for them yet. Changed
     * through {@link #add} and {@link #remove} only.
     */
    @Nonnull
    public Set<UUID> getIds(@Nonnull String key) {
        return record(key).getAllIds();
    }

    /**
     * @return whether the quest was not listed yet. The change is written at once.
     */
    public boolean add(@Nonnull String key, @Nonnull UUID questId) {
        QuestsRecord record = record(key);
        if (!record.register(questId)) return false;

        writer.flushIndex(record, key);
        return true;
    }

    /**
     * @return whether the quest was listed. The change is written at once.
     */
    public boolean remove(@Nonnull String key, @Nonnull UUID questId) {
        QuestsRecord record = record(key);
        if (!record.unregister(questId)) return false;

        writer.flushIndex(record, key);
        return true;
    }

    /**
     * @return the quests listed under that key, read back in one go if need be. An id nothing
     * answers to is taken off, but for a quest set aside, which comes back with its asset.
     */
    @Nonnull
    public List<AbstractQuestProgression<?>> resolve(@Nonnull String key) {
        List<UUID> questIds = List.copyOf(getIds(key));
        QuestProgressionService.get().loadQuests(questIds);

        List<AbstractQuestProgression<?>> quests = new ArrayList<>(questIds.size());
        for (UUID questId : questIds) {
            AbstractQuestProgression<?> quest = QuestProgressionService.get().loadQuest(questId);
            if (quest != null) {
                quests.add(quest);
            } else if (!QuestProgressionService.get().isSetAside(questId)) {
                remove(key, questId);
            }
        }
        return quests;
    }

    /**
     * @return the keys read back so far under that prefix, which are the ones worth following.
     */
    @Nonnull
    public Set<String> getLoadedKeys(@Nonnull String prefix) {
        Set<String> keys = new HashSet<>();
        for (String key : records.keySet()) {
            if (key.startsWith(prefix)) keys.add(key);
        }
        return keys;
    }

    /**
     * Takes in what other servers wrote under those keys since, read in one go. Keys never read
     * back here are left alone: nothing in memory follows them.
     *
     * @return for each key, the ids other servers added.
     */
    @Nonnull
    public Map<String, Set<UUID>> refresh(@Nonnull Collection<String> keys) {
        Map<String, Set<UUID>> added = new HashMap<>();
        storage.loadIndexes(keys).forEach((key, stored) -> {
            QuestsRecord record = records.get(key);
            if (record == null) return;

            Set<UUID> theirs = record.absorb(stored).added();
            if (!theirs.isEmpty()) added.put(key, theirs);
        });
        return added;
    }

    /**
     * Lets go of an index nothing will read again, such as a closed world's, here and in the
     * storage, the deletion written in order with the rest.
     */
    public void delete(@Nonnull String key) {
        records.remove(key);
        writer.deleteIndex(key);
    }

    /**
     * Writes what is still unwritten in every index, for the save pass and the shutdown: an index
     * that wrote its last change already writes nothing.
     */
    public void flushAll() {
        records.forEach((key, record) -> record.flush(storage, key));
    }

    /**
     * Read outside the map's lock, which a slow storage would otherwise hold for every key.
     */
    @Nonnull
    private QuestsRecord record(@Nonnull String key) {
        QuestsRecord record = records.get(key);
        if (record != null) return record;

        QuestsRecord read = new QuestsRecord();
        read.load(storage.loadIndex(key));

        QuestsRecord raced = records.putIfAbsent(key, read);
        return raced != null ? raced : read;
    }

    /**
     * Writes the changes of an index in the order they were made, off the game threads.
     */
    public interface Writer {
        /**
         * Writes what changed in that index since it was last written.
         */
        void flushIndex(@Nonnull QuestsRecord record, @Nonnull String key);

        /**
         * Lets go of an index nothing will read again.
         */
        void deleteIndex(@Nonnull String key);
    }
}
