package com.martelstudios.openquests.extension.rewards.random;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.OpenQuestsServices;
import com.martelstudios.openquests.core.rewards.QuestReward;
import com.martelstudios.openquests.extension.journal.QuestPageService;

import javax.annotation.Nonnull;

/**
 * Grants one reward drawn at random on completion.
 */
public final class RandomRewardFeature {
    public static final String TYPE_ID = "Random";

    private RandomRewardFeature() {}

    /**
     * Registers the reward type and its journal preview.
     */
    public static void register(@Nonnull JavaPlugin plugin, @Nonnull OpenQuestsServices services) {
        QuestReward.CODEC.register(TYPE_ID, RandomReward.class, RandomReward.CODEC);

        QuestPageService.register(new RandomRewardRenderer());
    }
}
