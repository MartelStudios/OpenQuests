package com.martelstudios.openquests.mmoskilltree.rewards.title;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.rewards.QuestReward;

import javax.annotation.Nonnull;

/**
 * A title the player unlocks.
 */
public final class TitleRewardFeature {
    public static final String TYPE_ID = "MMOSkillTreeTitle";

    private TitleRewardFeature() {}

    /**
     * Registers the reward. Its journal line is the one every CommonLib reward shares.
     */
    public static void register(@Nonnull JavaPlugin plugin) {
        QuestReward.CODEC.register(TYPE_ID, MMOSkillTreeTitleReward.class, MMOSkillTreeTitleReward.CODEC);
    }
}
