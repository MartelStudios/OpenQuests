package com.martelstudios.openquests.mmoskilltree.rewards.reputation;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.rewards.QuestReward;
import com.martelstudios.openquests.extension.journal.QuestPageService;

import javax.annotation.Nonnull;

/**
 * Standing with a reputation, such as the Mastery Trainer's favour.
 */
public final class ReputationRewardFeature {
    public static final String TYPE_ID = "MMOSkillTreeReputation";

    private ReputationRewardFeature() {}

    /**
     * Registers the reward and its line in the journal.
     */
    public static void register(@Nonnull JavaPlugin plugin) {
        QuestReward.CODEC.register(TYPE_ID, ReputationReward.class, ReputationReward.CODEC);

        QuestPageService.register(new ReputationRewardRenderer());
    }
}
