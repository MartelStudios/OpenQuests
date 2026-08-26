package com.martelstudios.openquests.extension.rewards.command;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.rewards.QuestReward;

import javax.annotation.Nonnull;

/**
 * Runs a server command on completion.
 */
public final class CommandRewardFeature {
    public static final String TYPE_ID = "Command";

    private CommandRewardFeature() {}

    public static void register(@Nonnull JavaPlugin plugin) {
        QuestReward.CODEC.register(TYPE_ID, CommandReward.class, CommandReward.CODEC);
    }
}
