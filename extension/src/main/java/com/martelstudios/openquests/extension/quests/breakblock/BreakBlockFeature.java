package com.martelstudios.openquests.extension.quests.breakblock;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.services.QuestProgressionService;

import javax.annotation.Nonnull;

/**
 * Break a number of blocks.
 */
public final class BreakBlockFeature {
    public static final String TYPE_ID = "BreakBlock";

    private BreakBlockFeature() {}

    public static void register(@Nonnull JavaPlugin plugin) {
        QuestProgressionService.get()
                               .registerQuestType(TYPE_ID, BreakBlockQuestAsset.class, BreakBlockQuestAsset.CODEC, BreakBlockQuestProgression.class, BreakBlockQuestProgression.CODEC);

        plugin.getEntityStoreRegistry().registerSystem(new BreakBlockEventSystem());
    }
}
