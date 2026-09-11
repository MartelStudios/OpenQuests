package com.martelstudios.openquests.core.rewards.stores;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.set.SetCodec;
import com.martelstudios.openquests.core.rewards.models.PendingRewards;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * What one player is still owed, keyed by the id the quest had while it was live. One entry per
 * completion rather than per asset, so a repeatable quest is collected run by run.
 */
public class PendingRewardStore {

    /**
     * Serialized as a flat set since each entry already carries its quest id; the map is rebuilt
     * from it on decode, which avoids storing the key twice and the deprecated map codecs.
     */
    public static final BuilderCodec<PendingRewardStore> CODEC = BuilderCodec.builder(PendingRewardStore.class, PendingRewardStore::new)
                                                                            .append(new KeyedCodec<>("Pending", new SetCodec<>(PendingRewards.CODEC, HashSet<PendingRewards>::new, false)), (store, owed) -> owed.forEach(store::owe), store -> new HashSet<>(store.owed.values()))
                                                                            .add()
                                                                            .build();

    private final Map<UUID, PendingRewards> owed = new ConcurrentHashMap<>();

    public PendingRewardStore() {}

    public PendingRewardStore(@Nonnull PendingRewardStore other) {
        this.owed.putAll(other.owed);
    }

    /**
     * Writes down a debt. An entry owing nothing is not kept: nothing to collect is the same as no
     * entry at all, and a store of settled debts would only grow.
     */
    public void owe(@Nonnull PendingRewards pending) {
        if (pending.isSettled()) {
            this.owed.remove(pending.getQuestId());
            return;
        }
        this.owed.put(pending.getQuestId(), pending);
    }

    @Nullable
    public PendingRewards get(@Nonnull UUID questId) {
        return this.owed.get(questId);
    }

    /**
     * @return whether that completion still has anything to hand over.
     */
    public boolean isOwed(@Nonnull UUID questId) {
        return this.owed.containsKey(questId);
    }

    public boolean settle(@Nonnull UUID questId) {
        return this.owed.remove(questId) != null;
    }

    /**
     * @return every debt still standing, as a copy: collecting one settles it, and the caller is
     * walking this while that happens.
     */
    @Nonnull
    public Collection<PendingRewards> getAll() {
        return List.copyOf(this.owed.values());
    }

    @Nonnull
    public List<UUID> getQuestIds() {
        return new ArrayList<>(this.owed.keySet());
    }

    @Nonnull
    public PendingRewardStore clone() {
        return new PendingRewardStore(this);
    }
}
