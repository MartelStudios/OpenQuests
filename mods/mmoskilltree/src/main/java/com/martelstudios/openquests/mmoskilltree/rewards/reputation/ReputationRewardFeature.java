package com.martelstudios.openquests.mmoskilltree.rewards.reputation;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.rewards.QuestReward;

import javax.annotation.Nonnull;

/**
 * Standing with a reputation, such as the Mastery Trainer's favour.
 */
public final class ReputationRewardFeature {
    public static final String TYPE_ID = "MMOSkillTreeReputation";

    private ReputationRewardFeature() {}

    /**
     * Registers the reward. Its journal line is the one every CommonLib reward shares.
     */
    public static void register(@Nonnull JavaPlugin plugin) {
        QuestReward.CODEC.register(TYPE_ID, MMOSkillTreeReputationReward.class, MMOSkillTreeReputationReward.CODEC);
    }
}
