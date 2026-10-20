package com.martelstudios.openquests.extension.quests.reachlocation;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.openquests.core.OpenQuestsServices;
import com.martelstudios.openquests.extension.listener.QuestListenerService;

import javax.annotation.Nonnull;

/**
 * Enter a radius around a position.
 */
public final class ReachLocationFeature {
    public static final String TYPE_ID = "ReachLocation";

    private static ComponentType<EntityStore, ReachLocationQuestListener> listenerType;

    private ReachLocationFeature() {}

    /**
     * @return the component naming a player's reach-location quests, which holds no meaning
     * outside a registered feature.
     */
    public static ComponentType<EntityStore, ReachLocationQuestListener> getListenerType() {
        return listenerType;
    }

    public static void register(@Nonnull JavaPlugin plugin, @Nonnull OpenQuestsServices services, @Nonnull QuestListenerService listeners) {
        services.getQuestProgressionService().registerQuestType(TYPE_ID, ReachLocationQuestAsset.class, ReachLocationQuestAsset.CODEC, ReachLocationQuestProgression.class, ReachLocationQuestProgression.CODEC);

        // Registered without a codec, which is what keeps it out of the player's file: it says
        // nothing the quest store does not already say, and is built back on connection
        listenerType = plugin.getEntityStoreRegistry().registerComponent(ReachLocationQuestListener.class, ReachLocationQuestListener::new);
        listeners.register(ReachLocationQuestProgression.class, listenerType);

        plugin.getEntityStoreRegistry().registerSystem(new ReachLocationTickingSystem(services.getQuestProgressionService()));
    }
}
