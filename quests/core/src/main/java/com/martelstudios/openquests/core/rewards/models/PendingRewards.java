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
 * Rewards that have not yet been claimed by a player.
 */
public class PendingRewards {

    public static final BuilderCodec<PendingRewards> CODEC = BuilderCodec.builder(PendingRewards.class, PendingRewards::new)
                                                                         .append(new KeyedCodec<>("QuestId", Codec.UUID_STRING), (owed, questId) -> owed.questId = questId, owed -> owed.questId)
                                                                         .add()
                                                                         .append(new KeyedCodec<>("Rewards", new ArrayCodec<>(QuestReward.CODEC, QuestReward[]::new)), (owed, rewards) -> owed.rewards = rewards, owed -> owed.rewards)
                                                                         .add()
                                                                         .build();

    public static final QuestReward[] NO_REWARDS = new QuestReward[0];

    /**
     * The id the quest had while it was live
     */
    protected UUID questId;

    protected QuestReward[] rewards = NO_REWARDS;

    private PendingRewards() {}

    public PendingRewards(@Nonnull AbstractQuestProgression<?> quest, @Nonnull QuestReward[] rewards) {
        this.questId = quest.getId();
        this.rewards = rewards.clone();
    }

    @Nonnull
    public UUID getQuestId() {
        return questId;
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
    public boolean isEmpty() {
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
