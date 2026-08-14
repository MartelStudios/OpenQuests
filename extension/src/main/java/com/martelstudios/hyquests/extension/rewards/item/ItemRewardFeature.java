package com.martelstudios.hyquests.extension.rewards.item;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.hyquests.core.rewards.QuestReward;

import javax.annotation.Nonnull;

/**
 * Gives items on completion.
 */
public final class ItemRewardFeature {
    public static final String TYPE_ID = "Item";

    private ItemRewardFeature() {}

    public static void register(@Nonnull JavaPlugin plugin) {
        QuestReward.CODEC.register(TYPE_ID, ItemQuestReward.class, ItemQuestReward.CODEC);
    }
}
