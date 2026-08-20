package com.martelstudios.hyquests.extension.quests.useentity;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.hyquests.core.services.QuestProgressionService;

import javax.annotation.Nonnull;

/**
 * Interact with NPCs of a group a number of times.
 */
public final class UseEntityFeature {
    public static final String TYPE_ID = "UseEntity";

    private UseEntityFeature() {}

    public static void register(@Nonnull JavaPlugin plugin) {
        QuestProgressionService.get()
                               .registerQuestType(TYPE_ID, UseEntityQuestAsset.class, UseEntityQuestAsset.CODEC, UseEntityQuestProgression.class, UseEntityQuestProgression.CODEC);

        plugin.getEntityStoreRegistry().registerSystem(new UseEntityEventSystem());
    }
}
