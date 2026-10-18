package com.martelstudios.openquests.mmoskilltree.rewards.effect;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.rewards.QuestReward;

import javax.annotation.Nonnull;

/**
 * An entity effect put on the player.
 */
public final class EffectRewardFeature {
    public static final String TYPE_ID = "MMOSkillTreeEffect";

    private EffectRewardFeature() {}

    /**
     * Registers the reward. Its journal line is the one every CommonLib reward shares.
     */
    public static void register(@Nonnull JavaPlugin plugin) {
        QuestReward.CODEC.register(TYPE_ID, MMOSkillTreeEffectReward.class, MMOSkillTreeEffectReward.CODEC);
    }
}
