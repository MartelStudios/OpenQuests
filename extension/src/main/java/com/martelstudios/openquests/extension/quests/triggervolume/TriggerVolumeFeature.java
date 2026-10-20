package com.martelstudios.openquests.extension.quests.triggervolume;

import com.hypixel.hytale.builtin.triggervolumes.event.TriggerVolumeEvent;
import com.hypixel.hytale.server.core.asset.LoadAssetEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.OpenQuestsServices;

import javax.annotation.Nonnull;

/**
 * Enter or leave trigger volumes whose name matches a regular expression, such as the ones a prefab places on
 * top of a tower, wherever a generated world put it.
 */
public final class TriggerVolumeFeature {
    public static final String TYPE_ID = "TriggerVolume";

    private TriggerVolumeFeature() {}

    public static void register(@Nonnull JavaPlugin plugin, @Nonnull OpenQuestsServices services) {
        services.getQuestProgressionService().registerQuestType(TYPE_ID, TriggerVolumeQuestAsset.class, TriggerVolumeQuestAsset.CODEC, TriggerVolumeQuestProgression.class, TriggerVolumeQuestProgression.CODEC);

        TriggerVolumeQuestEvents events = new TriggerVolumeQuestEvents(services.getQuestProgressionService());
        plugin.getEventRegistry()
              .registerGlobal(TriggerVolumeEvent.class, events::handleTriggerVolume);

        plugin.getEventRegistry()
              .registerGlobal(LoadAssetEvent.PRIORITY_LOAD_LATE, LoadAssetEvent.class, TriggerVolumeQuestEvents::handleLoadAsset);
    }
}
