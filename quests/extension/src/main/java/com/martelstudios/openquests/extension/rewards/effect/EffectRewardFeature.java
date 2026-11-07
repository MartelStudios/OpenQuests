package com.martelstudios.openquests.extension.rewards.effect;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.OpenQuestsServices;
import com.martelstudios.openquests.core.rewards.QuestReward;
import com.martelstudios.openquests.extension.journal.QuestPageService;

import javax.annotation.Nonnull;

/**
 * Puts an entity effect on the player on completion.
 */
public final class EffectRewardFeature {
    public static final String TYPE_ID = "Effect";

    private EffectRewardFeature() {}

    /**
     * Registers the reward and its journal line.
     */
    public static void register(@Nonnull JavaPlugin plugin, @Nonnull OpenQuestsServices services) {
        QuestReward.CODEC.register(TYPE_ID, EffectReward.class, EffectReward.CODEC);

        QuestPageService.register(new EffectRewardRenderer());
    }
}
