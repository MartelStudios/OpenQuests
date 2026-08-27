package com.martelstudios.openquests.extension.quests.quantity;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.extension.hud.QuestHudService;

import javax.annotation.Nonnull;

/**
 * What every counted quest type shares. Registers no quest type of its own: the concrete ones do
 * that, and only their common presentation lives here.
 */
public final class QuantityFeature {

    private QuantityFeature() {}

    public static void register(@Nonnull JavaPlugin plugin) {
        QuestHudService.register(new QuantityQuestHudRenderer());
    }
}
