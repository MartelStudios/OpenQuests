package com.martelstudios.openquests.mmoskilltree.rewards.lamp;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.rewards.QuestReward;

import javax.annotation.Nonnull;

/**
 * An MMO Skill Tree skill lamp.
 */
public final class LampRewardFeature {
    public static final String TYPE_ID = "MMOSkillTreeLamp";

    private LampRewardFeature() {}

    /**
     * Registers the reward. Its journal line is the one every CommonLib reward shares.
     */
    public static void register(@Nonnull JavaPlugin plugin) {
        QuestReward.CODEC.register(TYPE_ID, MMOSkillTreeLampReward.class, MMOSkillTreeLampReward.CODEC);
    }
}
