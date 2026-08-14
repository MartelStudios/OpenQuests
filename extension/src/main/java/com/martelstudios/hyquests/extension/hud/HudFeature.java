package com.martelstudios.hyquests.extension.hud;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;

import javax.annotation.Nonnull;

/**
 * The tracker panel listing a player's in-progress quests. A presentation choice rather than part
 * of the quest system, so a server wanting its own UI simply leaves this one out.
 */
public final class HudFeature {

    private HudFeature() {}

    public static void register(@Nonnull JavaPlugin plugin) {
        plugin.getEntityStoreRegistry().registerSystem(new QuestHudTickingSystem());
    }
}
