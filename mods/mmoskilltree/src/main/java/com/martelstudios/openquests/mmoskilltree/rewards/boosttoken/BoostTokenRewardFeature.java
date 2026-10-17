package com.martelstudios.openquests.mmoskilltree.rewards.boosttoken;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.rewards.QuestReward;

import javax.annotation.Nonnull;

/**
 * An MMO Skill Tree XP boost token.
 */
public final class BoostTokenRewardFeature {
    public static final String TYPE_ID = "MMOSkillTreeBoostToken";

    private BoostTokenRewardFeature() {}

    /**
     * Registers the reward. Its journal line is the one every CommonLib reward shares.
     */
    public static void register(@Nonnull JavaPlugin plugin) {
        QuestReward.CODEC.register(TYPE_ID, MMOSkillTreeBoostTokenReward.class, MMOSkillTreeBoostTokenReward.CODEC);
    }
}
