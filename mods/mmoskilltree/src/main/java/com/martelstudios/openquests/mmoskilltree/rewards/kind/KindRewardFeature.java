package com.martelstudios.openquests.mmoskilltree.rewards.kind;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.rewards.QuestReward;
import com.martelstudios.openquests.extension.journal.QuestPageService;

import javax.annotation.Nonnull;

/**
 * Rewards paid through MMO Skill Tree's CommonLib: mastery points and the Mastery Trainer's favour.
 */
public final class KindRewardFeature {
    public static final String CURRENCY_TYPE_ID = "MMOSkillTreeCurrency";
    public static final String REPUTATION_TYPE_ID = "MMOSkillTreeReputation";

    private KindRewardFeature() {}

    /**
     * Registers both rewards and the journal line they share.
     */
    public static void register(@Nonnull JavaPlugin plugin) {
        QuestReward.CODEC.register(CURRENCY_TYPE_ID, CurrencyReward.class, CurrencyReward.CODEC);
        QuestReward.CODEC.register(REPUTATION_TYPE_ID, ReputationReward.class, ReputationReward.CODEC);

        QuestPageService.register(new KindRewardRenderer());
    }
}
