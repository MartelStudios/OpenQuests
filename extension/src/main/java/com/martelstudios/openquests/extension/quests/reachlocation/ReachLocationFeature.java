package com.martelstudios.openquests.extension.quests.reachlocation;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.services.QuestProgressionService;

import javax.annotation.Nonnull;

/**
 * Enter a radius around a position.
 */
public final class ReachLocationFeature {
    public static final String TYPE_ID = "ReachLocation";

    private ReachLocationFeature() {}

    public static void register(@Nonnull JavaPlugin plugin) {
        QuestProgressionService.get()
                               .registerQuestType(TYPE_ID, ReachLocationQuestAsset.class, ReachLocationQuestAsset.CODEC, ReachLocationQuestProgression.class, ReachLocationQuestProgression.CODEC);

        plugin.getEntityStoreRegistry().registerSystem(new ReachLocationTickingSystem());
    }
}
