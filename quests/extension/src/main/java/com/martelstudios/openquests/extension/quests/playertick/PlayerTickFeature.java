package com.martelstudios.openquests.extension.quests.playertick;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.openquests.core.OpenQuestsServices;
import com.martelstudios.openquests.extension.listener.QuestListenerService;

import javax.annotation.Nonnull;

/**
 * Registers the shared player tick system and listener.
 */
public final class PlayerTickFeature {
    private static ComponentType<EntityStore, PlayerTickQuestListener> listenerType;

    private PlayerTickFeature() {}

    /**
     * @return the component tracking the player's quests that consume tick samples.
     */
    public static ComponentType<EntityStore, PlayerTickQuestListener> getListenerType() {
        return listenerType;
    }

    public static void register(@Nonnull JavaPlugin plugin, @Nonnull OpenQuestsServices services) {
        // Registered without a codec, so it stays out of the player's file, and on the base type,
        // so all of them share one component the way they share one system
        listenerType = plugin.getEntityStoreRegistry().registerComponent(PlayerTickQuestListener.class, PlayerTickQuestListener::new);
        QuestListenerService.register(PlayerTickQuestProgression.class, listenerType);

        plugin.getEntityStoreRegistry().registerSystem(new PlayerTickSystem(services.getQuestProgressionService()));
    }
}
