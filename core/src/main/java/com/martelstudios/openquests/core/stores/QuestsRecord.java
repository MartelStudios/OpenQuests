package com.martelstudios.openquests.core.stores;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The quests of one holder, by id — a player, a world, the universe. None of them holds the
 * quests themselves: a quest is one object whoever it belongs to.
 *
 * <p>In memory only. The {@link com.martelstudios.openquests.core.persistence.QuestStorage}
 * answers the same question from the other side: the players of a quest.
 */
public class QuestsRecord {

    private final Set<UUID> questIds = ConcurrentHashMap.newKeySet();

    public QuestsRecord() {

    }

    public QuestsRecord(@Nonnull QuestsRecord other) {
        this.questIds.addAll(other.questIds);
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
}
