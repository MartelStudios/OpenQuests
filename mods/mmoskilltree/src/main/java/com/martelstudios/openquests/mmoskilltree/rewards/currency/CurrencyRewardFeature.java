package com.martelstudios.openquests.mmoskilltree.rewards.currency;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.rewards.QuestReward;

import javax.annotation.Nonnull;

/**
 * A currency, such as mastery points.
 */
public final class CurrencyRewardFeature {
    public static final String TYPE_ID = "MMOSkillTreeCurrency";

    private CurrencyRewardFeature() {}

    /**
     * Registers the reward. Its journal line is the one every CommonLib reward shares.
     */
    public static void register(@Nonnull JavaPlugin plugin) {
        QuestReward.CODEC.register(TYPE_ID, MMOSkillTreeCurrencyReward.class, MMOSkillTreeCurrencyReward.CODEC);
    }
}
