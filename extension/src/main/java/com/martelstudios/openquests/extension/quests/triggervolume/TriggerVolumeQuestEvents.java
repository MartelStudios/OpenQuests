package com.martelstudios.openquests.extension.quests.triggervolume;

import com.hypixel.hytale.builtin.triggervolumes.TriggerVolumesPlugin;
import com.hypixel.hytale.builtin.triggervolumes.event.TriggerVolumeEvent;
import com.hypixel.hytale.server.core.asset.LoadAssetEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.core.stores.QuestStoreComponent;

import javax.annotation.Nonnull;

/**
 * Needs no system: the trigger volume plugin already says who entered or left what.
 */
public final class TriggerVolumeQuestEvents {

    private final QuestProgressionService progression;

    public TriggerVolumeQuestEvents(@Nonnull QuestProgressionService progression) {
        this.progression = progression;
    }

    /**
     * Counts the event for the player it names. Dispatched from the volumes' ticking system, so
     * already on the world thread the entity and the volume manager belong to.
     */
    public void handleTriggerVolume(@Nonnull TriggerVolumeEvent event) {
        var entityRef = event.getEntityRef();
        if (!entityRef.isValid()) return;

        var store = entityRef.getStore();
        var playerRef = store.getComponent(entityRef, PlayerRef.getComponentType());
        var questStore = store.getComponent(entityRef, QuestStoreComponent.getComponentType());
        if (playerRef == null || questStore == null) return;

        var volume = store.getResource(TriggerVolumesPlugin.get().getManagerResourceType()).getVolume(event.getVolumeId());
        if (volume == null || volume.getName() == null) return;

        progression.progress(new TriggerVolumeQuestVisitor(playerRef.getUuid(), event.getTriggerEventType(), volume.getName()), questStore.getQuestIds());
    }

    /**
     * Refuses at boot, once per faulty asset, a quest no volume would ever count.
     */
    public static void handleLoadAsset(@Nonnull LoadAssetEvent event) {
        for (OpenQuestAsset asset : OpenQuestAsset.getAssetMap().getAssetMap().values()) {
            if (!(asset instanceof TriggerVolumeQuestAsset volumeAsset)) continue;

            String error = volumeAsset.findError();
            if (error != null) event.failed(true, "Quest asset '" + asset.getId() + "' " + error);
        }
    }
}
