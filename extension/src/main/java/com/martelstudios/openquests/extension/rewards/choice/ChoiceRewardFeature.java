package com.martelstudios.openquests.extension.rewards.choice;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.OpenQuestsServices;
import com.martelstudios.openquests.core.rewards.QuestReward;
import com.martelstudios.openquests.extension.journal.QuestPageService;

import javax.annotation.Nonnull;

/**
 * Lets the player pick their rewards on completion.
 */
public final class ChoiceRewardFeature {
    public static final String TYPE_ID = "Choice";

    private ChoiceRewardFeature() {}

    /**
     * Registers the reward type and its journal preview.
     */
    public static void register(@Nonnull JavaPlugin plugin, @Nonnull OpenQuestsServices services) {
        QuestReward.CODEC.register(TYPE_ID, ChoiceReward.class, ChoiceReward.CODEC);

        QuestPageService.register(new ChoiceRewardRenderer());
    }
}
