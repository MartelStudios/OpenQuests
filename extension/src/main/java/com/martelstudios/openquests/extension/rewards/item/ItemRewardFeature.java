package com.martelstudios.openquests.extension.rewards.item;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.rewards.QuestReward;
import com.martelstudios.openquests.extension.journal.QuestPageService;

import javax.annotation.Nonnull;

/**
 * Gives items on completion.
 */
public final class ItemRewardFeature {
    public static final String TYPE_ID = "Item";

    private ItemRewardFeature() {}

    public static void register(@Nonnull JavaPlugin plugin) {
        QuestReward.CODEC.register(TYPE_ID, ItemReward.class, ItemReward.CODEC);

        QuestPageService.register(new ItemRewardRenderer());
    }
}
