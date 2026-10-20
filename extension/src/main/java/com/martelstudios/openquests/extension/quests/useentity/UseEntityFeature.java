package com.martelstudios.openquests.extension.quests.useentity;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.OpenQuestsServices;

import javax.annotation.Nonnull;

/**
 * Interact with NPCs of a group a number of times.
 */
public final class UseEntityFeature {
    public static final String TYPE_ID = "UseEntity";

    private UseEntityFeature() {}

    public static void register(@Nonnull JavaPlugin plugin, @Nonnull OpenQuestsServices services) {
        services.getQuestProgressionService().registerQuestType(TYPE_ID, UseEntityQuestAsset.class, UseEntityQuestAsset.CODEC, UseEntityQuestProgression.class, UseEntityQuestProgression.CODEC);

        plugin.getEntityStoreRegistry().registerSystem(new UseEntityEventSystem(services.getQuestProgressionService()));
    }
}
