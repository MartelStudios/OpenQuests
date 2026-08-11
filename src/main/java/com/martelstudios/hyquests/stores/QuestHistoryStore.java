package com.martelstudios.hyquests.stores;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.set.SetCodec;
import com.martelstudios.hyquests.models.QuestHistoryRecord;
import com.martelstudios.hyquests.models.QuestState;

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
 * The completed quests of a scope, keyed by the id the quest had while it was live. One record
 * per completion rather than one per asset, so a repeatable quest keeps a distinct claim state
 * and date for each run.
 */
public class QuestHistoryStore {

    /**
     * Serialized as a flat set since each record already carries its own id; the map is rebuilt
     * from it on decode, which avoids storing the key twice and the deprecated map codecs.
     */
    public static final BuilderCodec<QuestHistoryStore> CODEC = BuilderCodec.builder(QuestHistoryStore.class, QuestHistoryStore::new)
                                                                            .append(new KeyedCodec<>("Records", new SetCodec<>(QuestHistoryRecord.CODEC, HashSet<QuestHistoryRecord>::new, false)), (store, records) -> records.forEach(store::register), store -> new HashSet<>(store.records.values()))
                                                                            .add()
                                                                            .build();

    private final Map<UUID, QuestHistoryRecord> records = new ConcurrentHashMap<>();

    public QuestHistoryStore() {}

    public QuestHistoryStore(QuestHistoryStore other) {
        this.records.putAll(other.records);
    }

    /**
     * @return {@code true} if that completion was not already recorded.
     */
    public boolean register(@Nonnull QuestHistoryRecord record) {
        return this.records.putIfAbsent(record.getId(), record) == null;
    }

    /**
     * @param questId the id the quest had while it was live
     */
    @Nullable
    public QuestHistoryRecord get(@Nonnull UUID questId) {
        return this.records.get(questId);
    }

    /**
     * @return the live records, unordered: sort on {@link QuestHistoryRecord#getCompletedAt()}
     * to present them chronologically.
     */
    @Nonnull
    public Collection<QuestHistoryRecord> getAll() {
        return this.records.values();
    }

    @Nonnull
    public List<QuestHistoryRecord> getForAsset(@Nonnull String questAssetId) {
        List<QuestHistoryRecord> matches = new ArrayList<>();
        for (QuestHistoryRecord record : records.values()) {
            if (record.getQuestAssetId().equals(questAssetId)) matches.add(record);
        }
        return matches;
    }

    /**
     * @return {@code true} if the given quest was ever completed, whatever the outcome.
     */
    public boolean hasCompleted(@Nonnull String questAssetId) {
        for (QuestHistoryRecord record : records.values()) {
            if (record.getQuestAssetId().equals(questAssetId)) return true;
        }
        return false;
    }

    /**
     * Replaces the lossy "times completed" counter: outcomes stay individually countable.
     */
    public int count(@Nonnull String questAssetId, @Nonnull QuestState state) {
        int count = 0;
        for (QuestHistoryRecord record : records.values()) {
            if (record.getQuestAssetId().equals(questAssetId) && record.getState() == state) count++;
        }
        return count;
    }

    @Nonnull
    public List<QuestHistoryRecord> getClaimable() {
        List<QuestHistoryRecord> claimable = new ArrayList<>();
        for (QuestHistoryRecord record : records.values()) {
            if (record.isClaimable()) claimable.add(record);
        }
        return claimable;
    }

    @Override
    public QuestHistoryStore clone() {
        return new QuestHistoryStore(this);
    }
}
