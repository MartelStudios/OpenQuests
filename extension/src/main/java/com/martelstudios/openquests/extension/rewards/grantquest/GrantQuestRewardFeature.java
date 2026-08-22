package com.martelstudios.openquests.extension.rewards.grantquest;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.rewards.QuestReward;

import javax.annotation.Nonnull;

/**
 * Grants further quests on completion.
 */
public final class GrantQuestRewardFeature {
    public static final String TYPE_ID = "GrantQuest";

    private GrantQuestRewardFeature() {}

    public static void register(@Nonnull JavaPlugin plugin) {
        QuestReward.CODEC.register(TYPE_ID, GrantQuestReward.class, GrantQuestReward.CODEC);
    }
}
