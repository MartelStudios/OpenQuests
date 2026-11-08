package com.martelstudios.openquests.extension.quests.reachlocation;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.openquests.core.OpenQuestsServices;
import com.martelstudios.openquests.core.events.QuestStateChangedEvent;
import com.martelstudios.openquests.extension.listener.QuestListenerService;

import javax.annotation.Nonnull;

/**
 * Enter a radius around a position, or a row of spots in order.
 */
public final class ReachLocationFeature {
    public static final String TYPE_ID = "ReachLocation";
    public static final String PATH_TYPE_ID = "ReachPath";

    private static ComponentType<EntityStore, ReachLocationQuestListener> listenerType;

    private ReachLocationFeature() {}

    /**
     * @return the component naming a player's reach-location quests, which holds no meaning
     * outside a registered feature.
     */
    public static ComponentType<EntityStore, ReachLocationQuestListener> getListenerType() {
        return listenerType;
    }

    /**
     * Registers both types, the listener they share, and their markers.
     */
    public static void register(@Nonnull JavaPlugin plugin, @Nonnull OpenQuestsServices services) {
        services.getQuestProgressionService().registerQuestType(TYPE_ID, ReachLocationQuestAsset.class, ReachLocationQuestAsset.CODEC, ReachLocationQuestProgression.class, ReachLocationQuestProgression.CODEC);
        services.getQuestProgressionService().registerQuestType(PATH_TYPE_ID, ReachPathQuestAsset.class, ReachPathQuestAsset.CODEC, ReachPathQuestProgression.class, ReachPathQuestProgression.CODEC);

        // Registered without a codec, which is what keeps it out of the player's file: it says
        // nothing the quest store does not already say, and is built back on connection
        listenerType = plugin.getEntityStoreRegistry().registerComponent(ReachLocationQuestListener.class, ReachLocationQuestListener::new);
        QuestListenerService.register(ReachLocationQuestProgression.class, listenerType);
        QuestListenerService.register(ReachPathQuestProgression.class, listenerType);

        QuestMarkers markers = new QuestMarkers();
        plugin.getEntityStoreRegistry().registerSystem(new ReachLocationTickingSystem(services.getQuestProgressionService(), markers));
        plugin.getEventRegistry().registerGlobal(QuestStateChangedEvent.class, event -> {
            if (event.getQuest().isOver()) markers.hide(event.getQuest().getId());
        });
        plugin.getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, event -> markers.forget(event.getPlayerRef().getUuid()));
    }
}
