package com.martelstudios.openquests.extension.rewards.droplist;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.OpenQuestsServices;
import com.martelstudios.openquests.core.rewards.QuestReward;
import com.martelstudios.openquests.extension.journal.QuestPageService;

import javax.annotation.Nonnull;

/**
 * Gives what one of the game's drop lists rolls on completion.
 */
public final class DroplistRewardFeature {
    public static final String TYPE_ID = "Droplist";

    private DroplistRewardFeature() {}

    /**
     * Registers the reward and its journal line.
     */
    public static void register(@Nonnull JavaPlugin plugin, @Nonnull OpenQuestsServices services) {
        QuestReward.CODEC.register(TYPE_ID, DroplistReward.class, DroplistReward.CODEC);

        QuestPageService.register(new DroplistRewardRenderer());
    }
}
