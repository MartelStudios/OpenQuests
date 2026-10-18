package com.martelstudios.openquests.mmoskilltree.rewards.lootable;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.rewards.QuestReward;

import javax.annotation.Nonnull;

/**
 * A CommonLib loot table, rolled as the reward is paid.
 */
public final class LootableRewardFeature {
    public static final String TYPE_ID = "MMOSkillTreeLootable";

    private LootableRewardFeature() {}

    /**
     * Registers the reward. Its journal line is the one every CommonLib reward shares.
     */
    public static void register(@Nonnull JavaPlugin plugin) {
        QuestReward.CODEC.register(TYPE_ID, MMOSkillTreeLootableReward.class, MMOSkillTreeLootableReward.CODEC);
    }
}
