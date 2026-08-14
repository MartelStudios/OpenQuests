package com.martelstudios.hyquests.core.models;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.martelstudios.hyquests.core.assets.QuestAsset;

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
                                                                             .append(new KeyedCodec<>("Claimed", Codec.BOOLEAN), (record, claimed) -> record.claimed = claimed, record -> Boolean.valueOf(record.claimed))
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
    protected boolean claimed;
    protected Instant completedAt;

    private QuestHistoryRecord() {}

    public QuestHistoryRecord(@Nonnull AbstractQuest<?> quest) {
        this.id = quest.getId();
        this.questAssetId = quest.getQuestAssetId();
        this.state = quest.getState();
        this.completedAt = Instant.now();
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

    public boolean isClaimed() {
        return claimed;
    }

    public void setClaimed(boolean claimed) {
        this.claimed = claimed;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public boolean isSuccessful() {
        return state == QuestState.SUCCESSFUL;
    }

    /**
     * Driven by the outcome's rewards rather than by success, since failing or abandoning a
     * quest may also grant something.
     *
     * @return {@code true} if rewards are still available to collect for this completion.
     */
    public boolean isClaimable() {
        if (claimed) return false;

        QuestAsset asset = QuestAsset.getAsset(questAssetId);
        return asset != null && asset.hasRewards(state);
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
