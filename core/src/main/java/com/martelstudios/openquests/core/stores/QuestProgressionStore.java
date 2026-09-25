package com.martelstudios.openquests.core.stores;

import com.hypixel.hytale.server.core.HytaleServer;
import com.martelstudios.openquests.core.events.QuestLoadedEvent;
import com.martelstudios.openquests.core.events.QuestUnloadedEvent;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import com.martelstudios.openquests.core.persistence.QuestStorage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The quests in memory, and nothing about where they are kept: a {@link QuestStorage} answers for
 * that, and this holds what has been read back, indexes it by type, and keeps what ended apart
 * from what is still running.
 */
public class QuestProgressionStore {

    @Nonnull
    private final Map<UUID, AbstractQuestProgression<?>> quests = new ConcurrentHashMap<>();

    /**
     * Quests that ended, held apart from the live ones rather than told apart by their state:
     * everything walking the store wants what can still move. Reachable by id all the same, so a
     * finished chain still opens and a prerequisite still answers.
     */
    @Nonnull
    private final Map<UUID, AbstractQuestProgression<?>> archived = new ConcurrentHashMap<>();

    @Nonnull
    private final Map<Class<?>, Set<UUID>> idsByType = new ConcurrentHashMap<>();

    @Nonnull
    private final QuestStorage storage;

    public QuestProgressionStore(@Nonnull QuestStorage storage) {
        this.storage = storage;
    }

    @Nonnull
    public QuestStorage getStorage() {
        return storage;
    }

    /**
     * A quest read back having already ended goes straight to the archive, so the split survives a
     * restart without being written down anywhere.
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
     * Drops a quest from memory, leaving it where it is stored: the next lookup by id reads it
     * back. What it changed is written out first, since nothing else will now hold it.
     *
     * @return {@code null} for a quest the store was not holding.
     */
    public AbstractQuestProgression<?> unload(@Nonnull UUID id) {
        AbstractQuestProgression<?> quest = quests.containsKey(id) ? quests.get(id) : archived.get(id);
        if (quest == null) return null;

        save(quest);

        if (remove(id) == null) archived.remove(id);

        HytaleServer.get()
                    .getEventBus()
                    .dispatchFor(QuestUnloadedEvent.class, id)
                    .dispatch(new QuestUnloadedEvent(quest));

        return quest;
    }

    /**
     * Moves a quest that ended out of the way of everything that walks the live ones, keeping it
     * answerable by id. What it is stored as does not change: only which map holds it, and the
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
     * Removes a quest from memory and does away with what was written of it.
     */
    public AbstractQuestProgression<?> removeAndDelete(@Nonnull UUID id) {
        AbstractQuestProgression<?> quest = remove(id);
        if (quest == null) quest = archived.remove(id);
        if (quest == null) return null;

        storage.deleteProgression(quest);
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
     * @return those of the given quests that are in memory, running or ended alike. Never reads
     * anything back: a caller resolving an index wants what is there, not what could be.
     */
    @Nonnull
    public List<AbstractQuestProgression<?>> resolveAll(@Nonnull Collection<UUID> questIds) {
        List<AbstractQuestProgression<?>> resolved = new ArrayList<>(questIds.size());

        for (UUID questId : questIds) {
            AbstractQuestProgression<?> quest = get(questId);
            if (quest != null) resolved.add(quest);
        }
        return resolved;
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
     * Writes one quest out, for a caller letting go of it. Everything else goes through
     * {@link #saveAll} or {@link #save(Collection)}: a backend charges per call, not per quest.
     */
    public void save(@Nonnull AbstractQuestProgression<?> quest) {
        save(List.of(quest));
    }

    /**
     * Writes out those of the given quests that changed and are meant to be kept, as one batch.
     *
     * <p>A batch that fails is marked dirty again, so a database down for a minute does not take
     * an hour of play with it.
     */
    public void save(@Nonnull Collection<AbstractQuestProgression<?>> candidates) {
        List<AbstractQuestProgression<?>> dirty = new ArrayList<>();

        for (AbstractQuestProgression<?> quest : candidates) {
            if (!isPersisted(quest)) continue;
            if (!quest.consumeChanges()) continue;

            dirty.add(quest);
        }

        if (dirty.isEmpty()) return;

        try {
            storage.saveProgressions(dirty);
        } catch (RuntimeException e) {
            for (AbstractQuestProgression<?> quest : dirty) {
                quest.markDirty();
            }

            throw e;
        }
    }

    /**
     * Writes out every quest that changed since the last pass.
     */
    public void saveAll() {
        List<AbstractQuestProgression<?>> candidates = new ArrayList<>(quests.size() + archived.size());
        candidates.addAll(quests.values());

        // The archive too: a quest is marked dirty by the very change that ended it
        candidates.addAll(archived.values());

        save(candidates);
    }

    /**
     * Pulls every stored quest into memory, rebuilding the type index.
     */
    public void loadAll() {
        for (AbstractQuestProgression<?> quest : storage.loadAllProgressions()) {
            add(quest);
        }
    }

    /**
     * Reads back everything one player takes part in, in one go: a quest at a time would make
     * connecting cost a round trip per quest.
     *
     * @return the quests now in memory for that player, those they were already holding included.
     */
    @Nonnull
    public List<AbstractQuestProgression<?>> loadForPlayer(@Nonnull UUID playerId) {
        List<AbstractQuestProgression<?>> loaded = storage.loadPlayerProgressions(playerId);

        for (AbstractQuestProgression<?> quest : loaded) {
            add(quest);
        }

        // add() keeps whichever instance was already there; the one just read is a copy
        List<AbstractQuestProgression<?>> held = new ArrayList<>(loaded.size());
        for (AbstractQuestProgression<?> quest : loaded) {
            AbstractQuestProgression<?> current = get(quest.getId());
            if (current != null) held.add(current);
        }
        return held;
    }

    /**
     * Reads a quest back in, or hands over the one already there. The one lookup allowed to cost
     * a read and to announce what it found.
     *
     * @return {@code null} for an id nothing answers to.
     */
    @Nullable
    public AbstractQuestProgression<?> load(@Nonnull UUID id) {
        AbstractQuestProgression<?> quest = quests.get(id);
        if (quest == null) quest = archived.get(id);
        if (quest != null) return quest;

        AbstractQuestProgression<?> read = storage.loadProgression(id);
        if (read == null) return null;

        add(read);

        // Whichever half add() put it in: one that ended came back to the archive
        return get(id);
    }

    /**
     * @return {@code true} if this quest is meant to survive a restart at all.
     */
    public static boolean isPersisted(@Nonnull AbstractQuestProgression<?> quest) {
        OpenQuestAsset asset = quest.getAsset();
        return asset == null || asset.isPersistProgression();
    }
}
