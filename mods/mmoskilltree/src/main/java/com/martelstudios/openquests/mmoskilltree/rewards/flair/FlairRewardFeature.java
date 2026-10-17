package com.martelstudios.openquests.mmoskilltree.rewards.flair;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.rewards.QuestReward;

import javax.annotation.Nonnull;

/**
 * A flair the player unlocks.
 */
public final class FlairRewardFeature {
    public static final String TYPE_ID = "MMOSkillTreeFlair";

    private FlairRewardFeature() {}

    /**
     * Registers the reward. Its journal line is the one every CommonLib reward shares.
     */
    public static void register(@Nonnull JavaPlugin plugin) {
        QuestReward.CODEC.register(TYPE_ID, MMOSkillTreeFlairReward.class, MMOSkillTreeFlairReward.CODEC);
    }
}
