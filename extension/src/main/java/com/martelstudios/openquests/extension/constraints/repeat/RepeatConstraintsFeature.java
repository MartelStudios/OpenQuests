package com.martelstudios.openquests.extension.constraints.repeat;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.constraints.QuestConstraint;

import javax.annotation.Nonnull;

/**
 * How often a player may be handed a quest from the same asset: at most once a period, or a
 * number of times in all. Both read the counts the core keeps per player and asset.
 */
public final class RepeatConstraintsFeature {
    public static final String COOLDOWN_TYPE_ID = "Cooldown";
    public static final String MAX_COMPLETIONS_TYPE_ID = "MaxCompletions";

    private RepeatConstraintsFeature() {}

    /**
     * Registers both constraints under their type ids.
     */
    public static void register(@Nonnull JavaPlugin plugin) {
        QuestConstraint.CODEC.register(COOLDOWN_TYPE_ID, CooldownConstraint.class, CooldownConstraint.CODEC);
        QuestConstraint.CODEC.register(MAX_COMPLETIONS_TYPE_ID, MaxCompletionsConstraint.class, MaxCompletionsConstraint.CODEC);
    }
}
