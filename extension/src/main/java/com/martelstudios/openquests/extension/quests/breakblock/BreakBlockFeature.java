package com.martelstudios.openquests.extension.quests.breakblock;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.OpenQuestsServices;

import javax.annotation.Nonnull;

/**
 * Break a number of blocks.
 */
public final class BreakBlockFeature {
    public static final String TYPE_ID = "BreakBlock";

    private BreakBlockFeature() {}

    public static void register(@Nonnull JavaPlugin plugin, @Nonnull OpenQuestsServices services) {
        services.getQuestProgressionService().registerQuestType(TYPE_ID, BreakBlockQuestAsset.class, BreakBlockQuestAsset.CODEC, BreakBlockQuestProgression.class, BreakBlockQuestProgression.CODEC);

        plugin.getEntityStoreRegistry().registerSystem(new BreakBlockEventSystem(services.getQuestProgressionService()));
    }
}
