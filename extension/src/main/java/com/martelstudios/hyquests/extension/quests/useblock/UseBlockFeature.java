package com.martelstudios.hyquests.extension.quests.useblock;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.hyquests.core.services.QuestProgressionService;

import javax.annotation.Nonnull;

/**
 * Interact with a block a number of times.
 */
public final class UseBlockFeature {
    public static final String TYPE_ID = "UseBlock";

    private UseBlockFeature() {}

    public static void register(@Nonnull JavaPlugin plugin) {
        QuestProgressionService.get()
                               .registerQuestType(TYPE_ID, UseBlockQuestAsset.class, UseBlockQuestAsset.CODEC, UseBlockQuestProgression.class, UseBlockQuestProgression.CODEC);

        plugin.getEntityStoreRegistry().registerSystem(new UseBlockEventSystem());
    }
}
