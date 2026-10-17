package com.martelstudios.openquests.mmoskilltree.rewards.costume;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.rewards.QuestReward;

import javax.annotation.Nonnull;

/**
 * A costume put on the player.
 */
public final class CostumeRewardFeature {
    public static final String TYPE_ID = "MMOSkillTreeCostume";

    private CostumeRewardFeature() {}

    /**
     * Registers the reward. Its journal line is the one every CommonLib reward shares.
     */
    public static void register(@Nonnull JavaPlugin plugin) {
        QuestReward.CODEC.register(TYPE_ID, MMOSkillTreeCostumeReward.class, MMOSkillTreeCostumeReward.CODEC);
    }
}
