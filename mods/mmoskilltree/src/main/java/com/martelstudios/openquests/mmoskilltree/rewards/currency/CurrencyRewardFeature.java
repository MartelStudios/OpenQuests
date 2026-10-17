package com.martelstudios.openquests.mmoskilltree.rewards.currency;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.rewards.QuestReward;
import com.martelstudios.openquests.extension.journal.QuestPageService;

import javax.annotation.Nonnull;

/**
 * A currency of MMO Skill Tree, such as mastery points.
 */
public final class CurrencyRewardFeature {
    public static final String TYPE_ID = "MMOSkillTreeCurrency";

    private CurrencyRewardFeature() {}

    /**
     * Registers the reward and its line in the journal.
     */
    public static void register(@Nonnull JavaPlugin plugin) {
        QuestReward.CODEC.register(TYPE_ID, CurrencyReward.class, CurrencyReward.CODEC);

        QuestPageService.register(new CurrencyRewardRenderer());
    }
}
