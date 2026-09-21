package com.martelstudios.openquests.core.rewards.stores;

import com.martelstudios.openquests.core.rewards.models.PendingRewards;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * What one player is still owed, keyed by the id the quest had while it was live. One entry per
 * completion rather than per asset, so a repeatable quest is collected run by run.
 */
public class PendingRewardStore {

    private final Map<UUID, PendingRewards> pending = new ConcurrentHashMap<>();

    private transient boolean dirty;

    public PendingRewardStore() {}

    public PendingRewardStore(@Nonnull PendingRewardStore other) {
        this.pending.putAll(other.pending);
        this.dirty = other.dirty;
    }

    public void add(@Nonnull PendingRewards pending) {
        this.pending.put(pending.getQuestId(), pending);
        markDirty();
    }

    public boolean remove(@Nonnull UUID questId) {
        if (this.pending.remove(questId) == null) return false;

        markDirty();
        return true;
    }

    @Nullable
    public PendingRewards get(@Nonnull UUID questId) {
        return this.pending.get(questId);
    }

    /**
     * @return whether that completion still has anything to hand over.
     */
    public boolean isOwed(@Nonnull UUID questId) {
        return this.pending.containsKey(questId);
    }

    /**
     * @return every debt still standing, as a copy: collecting one settles it while this is walked.
     */
    @Nonnull
    public Collection<PendingRewards> getAll() {
        return List.copyOf(this.pending.values());
    }

    @Nonnull
    public List<UUID> getQuestIds() {
        return new ArrayList<>(this.pending.keySet());
    }

    /**
     * @return what is owed, in the form a record is written from.
     */
    @Nonnull
    public Set<PendingRewards> snapshot() {
        return new HashSet<>(this.pending.values());
    }

    /**
     * Takes over what a player's record said, which is how a session starts.
     */
    public void restore(@Nonnull Collection<PendingRewards> owed) {
        this.pending.clear();

        for (PendingRewards rewards : owed) {
            this.pending.put(rewards.getQuestId(), rewards);
        }
        this.dirty = false;
    }

    /**
     * The entry is mutated in place rather than replaced, so the service says when it changed.
     */
    public void markDirty() {
        this.dirty = true;
    }

    /**
     * @return {@code true} if anything owed changed since the last write, without clearing the
     * flag.
     */
    public boolean hasChanges() {
        return dirty;
    }

    /**
     * @return {@code true} if anything owed changed since the last call, clearing the flag.
     */
    public boolean consumeChanges() {
        if (!dirty) return false;
        dirty = false;
        return true;
    }

    @Nonnull
    public PendingRewardStore clone() {
        return new PendingRewardStore(this);
    }
}
