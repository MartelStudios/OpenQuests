package com.martelstudios.openquests.core.rewards.models;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.rewards.QuestReward;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * What one completed quest still owes one player. Kept per player rather than on the quest: a
 * quest is shared by everyone holding it, and a debt is not.
 *
 * <p>The rewards themselves are held rather than a place in the asset's list. What was owed stays
 * owed even if the asset is rewritten between the moment the quest ended and the moment the player
 * comes to collect.
 */
public class PendingRewards {

    public static final BuilderCodec<PendingRewards> CODEC = BuilderCodec.builder(PendingRewards.class, PendingRewards::new)
                                                                        .append(new KeyedCodec<>("QuestId", Codec.UUID_BINARY), (owed, questId) -> owed.questId = questId, owed -> owed.questId)
                                                                        .add()
                                                                        .append(new KeyedCodec<>("QuestAssetId", Codec.STRING), (owed, assetId) -> owed.questAssetId = assetId, owed -> owed.questAssetId)
                                                                        .add()
                                                                        .append(new KeyedCodec<>("Rewards", new ArrayCodec<>(QuestReward.CODEC, QuestReward[]::new)), (owed, rewards) -> owed.rewards = rewards, owed -> owed.rewards)
                                                                        .add()
                                                                        .build();

    private static final QuestReward[] NO_REWARDS = new QuestReward[0];

    /** The id the quest had while it was live, which is what a claim names. */
    protected UUID questId;

    /**
     * What the quest was built from, kept here rather than looked up through the quest: a quest
     * that ended has left the store, and a debt that cannot say what it is for cannot be read.
     */
    protected String questAssetId;

    protected QuestReward[] rewards = NO_REWARDS;

    private PendingRewards() {}

    public PendingRewards(@Nonnull AbstractQuestProgression<?> quest, @Nonnull QuestReward[] rewards) {
        this.questId = quest.getId();
        this.questAssetId = quest.getAssetId();
        this.rewards = rewards.clone();
    }

    @Nonnull
    public UUID getQuestId() {
        return questId;
    }

    public String getQuestAssetId() {
        return questAssetId;
    }

    @Nonnull
    public QuestReward[] getRewards() {
        return rewards;
    }

    public void setRewards(@Nonnull QuestReward[] rewards) {
        this.rewards = rewards;
    }

    /**
     * @return {@code true} once nothing is left to hand over.
     */
    public boolean isSettled() {
        return rewards.length == 0;
    }

    /**
     * Identity is the quest id alone, so a completion stays unique in a set across reloads, where
     * the same debt is decoded into a new instance.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof PendingRewards owed)) return false;

        return questId.equals(owed.questId);
    }

    @Override
    public int hashCode() {
        return questId.hashCode();
    }
}
