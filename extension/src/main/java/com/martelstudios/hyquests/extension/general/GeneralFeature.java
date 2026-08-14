package com.martelstudios.hyquests.extension.general;

import com.hypixel.hytale.server.core.asset.LoadAssetEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.hyquests.core.services.QuestProgressionService;

import javax.annotation.Nonnull;

/**
 * Composite quest that succeeds once all of its children have completed. Needs no system of its
 * own: it reacts to its children through the events the core already publishes.
 */
public final class GeneralFeature {
    public static final String TYPE_ID = "General";

    private GeneralFeature() {}

    public static void register(@Nonnull JavaPlugin plugin) {
        QuestProgressionService.get()
                               .registerQuestType(TYPE_ID, GeneralQuestAsset.class, GeneralQuestAsset.CODEC, GeneralQuest.class, GeneralQuest.CODEC);

        // Referencing other assets is what makes this type able to loop, so it validates its own graph
        plugin.getEventRegistry()
              .registerGlobal(LoadAssetEvent.PRIORITY_LOAD_LATE, LoadAssetEvent.class, GeneralQuestAssetValidator::handleLoadAsset);
    }
}
