package com.martelstudios.openquests.core.stores;

import com.martelstudios.openquests.core.persistence.IndexStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * The quests of one holder, by id: a player, a world, the universe. None of them holds the
 * quests themselves: a quest is one object whoever it belongs to.
 *
 * <p>In memory only. The {@link com.martelstudios.openquests.core.persistence.QuestStorage}
 * answers the same question from the other side: the players of a quest.
 */
public class QuestsRecord {

    private final Set<UUID> questIds = ConcurrentHashMap.newKeySet();

    /**
     * What the storage holds under this record's key as far as this server knows, which is what a
     * write is told apart from: only what was added or removed since is written.
     */
    private final Set<UUID> stored = ConcurrentHashMap.newKeySet();

    public QuestsRecord() {

    }

    public QuestsRecord(@Nonnull QuestsRecord other) {
        this.questIds.addAll(other.questIds);
        this.stored.addAll(other.stored);
    }

    public QuestsRecord(@Nonnull Set<UUID> questIds) {
        this.questIds.addAll(questIds);
    }

    /**
     * @return {@code true} if the quest was not already registered.
     */
    public boolean register(@Nonnull UUID questId) {
        return this.questIds.add(questId);
    }

    public boolean unregister(@Nonnull UUID questId) {
        return this.questIds.remove(questId);
    }

    public boolean contains(@Nonnull UUID questId) {
        return this.questIds.contains(questId);
    }

    /**
     * Drops everything and takes these instead, which is what reading a scope back amounts to.
     */
    public void replaceAll(@Nonnull Set<UUID> questIds) {
        this.questIds.retainAll(questIds);
        this.questIds.addAll(questIds);
    }

    /**
     * Takes what the storage holds under this record's key, as read back from it.
     */
    public synchronized void load(@Nonnull Set<UUID> storedIds) {
        replaceAll(storedIds);
        stored.retainAll(storedIds);
        stored.addAll(storedIds);
    }

    /**
     * Writes what was added and removed since the last write or read, member by member, so a
     * server sharing the key loses nothing another one added meanwhile. One write or read at a
     * time, so a write never undoes what a read just took in.
     *
     * @return whether anything was written.
     */
    public synchronized boolean flush(@Nonnull IndexStore storage, @Nonnull String indexKey) {
        return flush(storage, indexKey, listed -> {});
    }

    /**
     * Writes as above, first handing the ids about to be listed to {@code beforeListing}, which
     * writes their quests: a server reading the index must find every quest it lists.
     *
     * @return whether anything was written.
     */
    public synchronized boolean flush(@Nonnull IndexStore storage, @Nonnull String indexKey, @Nonnull Consumer<Set<UUID>> beforeListing) {
        Set<UUID> current = Set.copyOf(questIds);

        Set<UUID> added = new HashSet<>(current);
        added.removeAll(stored);
        Set<UUID> removed = new HashSet<>(stored);
        removed.removeAll(current);
        if (added.isEmpty() && removed.isEmpty()) return false;

        if (!added.isEmpty()) beforeListing.accept(added);
        storage.addToIndex(indexKey, added);
        storage.removeFromIndex(indexKey, removed);

        stored.addAll(added);
        stored.removeAll(removed);
        return true;
    }

    /**
     * Takes in what the storage holds now under this record's key, as other servers wrote it:
     * their adds and removals. What this server added or removed and has not written yet stays as
     * it is, to be written next.
     *
     * @return the ids other servers added since, and those they removed.
     */
    @Nonnull
    public synchronized Absorbed absorb(@Nonnull Set<UUID> storedNow) {
        Set<UUID> added = new HashSet<>(storedNow);
        added.removeAll(stored);
        Set<UUID> removed = new HashSet<>(stored);
        removed.removeAll(storedNow);

        questIds.addAll(added);
        questIds.removeAll(removed);
        stored.addAll(added);
        stored.removeAll(removed);
        return new Absorbed(added, removed);
    }

    /**
     * @return the live set of registered quest ids.
     */
    @Nonnull
    public Set<UUID> getAllIds() {
        return this.questIds;
    }

    public boolean isEmpty() {
        return this.questIds.isEmpty();
    }

    @Nullable
    @Override
    public QuestsRecord clone() {
        return new QuestsRecord(this);
    }

    /**
     * What other servers changed under a key since this server last looked.
     *
     * @param added the ids they added
     * @param removed the ids they removed
     */
    public record Absorbed(@Nonnull Set<UUID> added, @Nonnull Set<UUID> removed) {}
}
