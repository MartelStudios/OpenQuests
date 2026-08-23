package com.martelstudios.openquests.core.history.models;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.martelstudios.openquests.core.models.QuestAsset;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.rewards.QuestReward;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.UUID;

/**
 * What is left of a quest once it completed and its instance was deleted. One record per
 * completion rather than one per asset, so a repeatable quest keeps a distinct claim state and
 * date for each run.
 */
public class QuestHistoryRecord {

    public static final BuilderCodec<QuestHistoryRecord> CODEC = BuilderCodec.builder(QuestHistoryRecord.class, QuestHistoryRecord::new)
                                                                             .append(new KeyedCodec<>("Id", Codec.UUID_BINARY), (record, id) -> record.id = id, record -> record.id)
                                                                             .add()
                                                                             .append(new KeyedCodec<>("QuestAssetId", Codec.STRING), (record, assetId) -> record.questAssetId = assetId, record -> record.questAssetId)
                                                                             .add()
                                                                             .append(new KeyedCodec<>("State", new EnumCodec<>(QuestState.class)), (record, state) -> record.state = state, record -> record.state)
                                                                             .add()
                                                                             .append(new KeyedCodec<>("PendingRewards", new ArrayCodec<>(QuestReward.CODEC, QuestReward[]::new)), (record, rewards) -> record.pendingRewards = rewards, record -> record.pendingRewards)
                                                                             .add()
                                                                             .append(new KeyedCodec<>("CompletedAt", Codec.LONG), (record, millis) -> record.completedAt = Instant.ofEpochMilli(millis), record -> record.completedAt == null ? null : Long.valueOf(record.completedAt.toEpochMilli()))
                                                                             .add()
                                                                             .build();

    /**
     * The completed quest's own id, so anything that referenced the live quest still resolves.
     */
    protected UUID id;
    protected String questAssetId;
    protected QuestState state;
    protected QuestReward[] pendingRewards = NO_REWARDS;
    protected Instant completedAt;

    private static final QuestReward[] NO_REWARDS = new QuestReward[0];

    private QuestHistoryRecord() {}

    public QuestHistoryRecord(@Nonnull AbstractQuestProgression<?> quest) {
        this.id = quest.getId();
        this.questAssetId = quest.getAssetId();
        this.state = quest.getState();
        this.completedAt = Instant.now();

        QuestAsset asset = quest.getAsset();
        if (asset != null) this.pendingRewards = asset.getRewards(state).clone();
    }

    public UUID getId() {
        return id;
    }

    public String getQuestAssetId() {
        return questAssetId;
    }

    public QuestAsset getAsset() {
        return QuestAsset.getAsset(questAssetId);
    }

    public QuestState getState() {
        return state;
    }

    /**
     * @return what this completion still owes, empty once everything was handed over.
     */
    @Nonnull
    public QuestReward[] getPendingRewards() {
        return pendingRewards;
    }

    public void setPendingRewards(@Nonnull QuestReward[] pendingRewards) {
        this.pendingRewards = pendingRewards;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public boolean isSuccessful() {
        return state == QuestState.SUCCESSFUL;
    }

    public boolean isFailed() {
        return state == QuestState.FAILED;
    }

    public boolean isAbandoned() {
        return state == QuestState.ABANDONED;
    }

    public boolean isCompleted() {
        return isSuccessful() || isFailed() || isAbandoned();
    }

    /**
     * @return {@code true} once nothing is left to hand over, whether there was anything to give
     * in the first place or everything was granted.
     */
    public boolean isClaimed() {
        return pendingRewards.length == 0;
    }

    /**
     * @return {@code true} if rewards are still available to collect for this completion.
     */
    public boolean isClaimable() {
        return pendingRewards.length > 0;
    }

    /**
     * Identity is the completed quest's id alone, so records stay unique in a set across
     * reloads, where a same completion is decoded into a new instance.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof QuestHistoryRecord record)) return false;
        return id.equals(record.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
