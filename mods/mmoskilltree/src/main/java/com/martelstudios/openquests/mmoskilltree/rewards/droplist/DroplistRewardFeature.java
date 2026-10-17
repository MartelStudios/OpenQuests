package com.martelstudios.openquests.mmoskilltree.rewards.droplist;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.rewards.QuestReward;

import javax.annotation.Nonnull;

/**
 * One of the game's drop lists, rolled a number of times.
 */
public final class DroplistRewardFeature {
    public static final String TYPE_ID = "MMOSkillTreeDroplist";

    private DroplistRewardFeature() {}

    /**
     * Registers the reward. Its journal line is the one every CommonLib reward shares.
     */
    public static void register(@Nonnull JavaPlugin plugin) {
        QuestReward.CODEC.register(TYPE_ID, MMOSkillTreeDroplistReward.class, MMOSkillTreeDroplistReward.CODEC);
    }
}
