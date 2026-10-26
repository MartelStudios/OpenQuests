package com.martelstudios.openquests.extension.quests.enterworld;

import com.hypixel.hytale.server.core.asset.LoadAssetEvent;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.OpenQuestsServices;

import javax.annotation.Nonnull;

/**
 * Enter a world whose name matches a regular expression.
 */
public final class EnterWorldFeature {
    public static final String TYPE_ID = "EnterWorld";

    private EnterWorldFeature() {}

    public static void register(@Nonnull JavaPlugin plugin, @Nonnull OpenQuestsServices services) {
        services.getQuestProgressionService().registerQuestType(TYPE_ID, EnterWorldQuestAsset.class, EnterWorldQuestAsset.CODEC, EnterWorldQuestProgression.class, EnterWorldQuestProgression.CODEC);

        EnterWorldQuestEvents events = new EnterWorldQuestEvents(services.getQuestProgressionService());
        plugin.getEventRegistry()
              .registerGlobal(AddPlayerToWorldEvent.class, events::handleAddPlayerToWorld);

        plugin.getEventRegistry()
              .registerGlobal(LoadAssetEvent.PRIORITY_LOAD_LATE, LoadAssetEvent.class, EnterWorldQuestAssetValidator::handleLoadAsset);
    }
}
