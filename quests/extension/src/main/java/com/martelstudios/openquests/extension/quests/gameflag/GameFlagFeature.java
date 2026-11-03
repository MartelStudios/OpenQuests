package com.martelstudios.openquests.extension.quests.gameflag;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.openquests.core.OpenQuestsServices;
import com.martelstudios.openquests.extension.listener.QuestListenerService;

import javax.annotation.Nonnull;

/**
 * Reach a level of a server game flag, the progress Hytale keeps for the goblin lab repairs.
 */
public final class GameFlagFeature {
    public static final String TYPE_ID = "GameFlag";

    private static ComponentType<EntityStore, GameFlagQuestListener> listenerType;

    private GameFlagFeature() {}

    /**
     * @return the component naming a player's game flag quests, which holds no meaning outside a
     * registered feature.
     */
    public static ComponentType<EntityStore, GameFlagQuestListener> getListenerType() {
        return listenerType;
    }

    /**
     * Registers the quest type, its player listener and the system checking flags every tick.
     */
    public static void register(@Nonnull JavaPlugin plugin, @Nonnull OpenQuestsServices services) {
        services.getQuestProgressionService().registerQuestType(TYPE_ID, GameFlagQuestAsset.class, GameFlagQuestAsset.CODEC, GameFlagQuestProgression.class, GameFlagQuestProgression.CODEC);

        // Codec-less, which keeps it out of the player file: it is built back on connection
        listenerType = plugin.getEntityStoreRegistry().registerComponent(GameFlagQuestListener.class, GameFlagQuestListener::new);
        QuestListenerService.register(GameFlagQuestProgression.class, listenerType);

        plugin.getEntityStoreRegistry().registerSystem(new GameFlagTickingSystem(services.getQuestProgressionService()));
    }
}
