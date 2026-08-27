package com.martelstudios.openquests.extension.quests.composite;

import com.hypixel.hytale.server.core.asset.LoadAssetEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.extension.hud.QuestHudService;

import javax.annotation.Nonnull;

/**
 * Composite quest that succeeds once all of its children have completed. Needs no system of its
 * own: it reacts to its children through the events the core already publishes.
 */
public final class CompositeFeature {
    public static final String TYPE_ID = "Composite";

    private CompositeFeature() {}

    public static void register(@Nonnull JavaPlugin plugin) {
        QuestProgressionService.get()
                               .registerQuestType(TYPE_ID, CompositeQuestAsset.class, CompositeQuestAsset.CODEC, CompositeQuestProgression.class, CompositeQuestProgression.CODEC);

        QuestHudService.register(new CompositeQuestHudRenderer());

        // Referencing other assets is what makes this type able to loop, so it validates its own graph
        plugin.getEventRegistry()
              .registerGlobal(LoadAssetEvent.PRIORITY_LOAD_LATE, LoadAssetEvent.class, CompositeQuestAssetValidator::handleLoadAsset);
    }
}
