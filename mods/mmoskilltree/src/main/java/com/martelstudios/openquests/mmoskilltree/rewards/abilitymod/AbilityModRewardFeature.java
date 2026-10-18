package com.martelstudios.openquests.mmoskilltree.rewards.abilitymod;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.rewards.QuestReward;

import javax.annotation.Nonnull;

/**
 * An MMO Skill Tree ability modifier.
 */
public final class AbilityModRewardFeature {
    public static final String TYPE_ID = "MMOSkillTreeAbilityMod";

    private AbilityModRewardFeature() {}

    /**
     * Registers the reward. Its journal line is the one every CommonLib reward shares.
     */
    public static void register(@Nonnull JavaPlugin plugin) {
        QuestReward.CODEC.register(TYPE_ID, MMOSkillTreeAbilityModReward.class, MMOSkillTreeAbilityModReward.CODEC);
    }
}
