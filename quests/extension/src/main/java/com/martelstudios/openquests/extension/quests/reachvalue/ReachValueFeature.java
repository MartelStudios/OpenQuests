package com.martelstudios.openquests.extension.quests.reachvalue;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.openquests.core.OpenQuestsServices;
import com.martelstudios.openquests.extension.listener.QuestListenerService;

import javax.annotation.Nonnull;

/**
 * Reach a value of the game that fires no event, such as a game flag level or a count of memories.
 */
public final class ReachValueFeature {
    public static final String TYPE_ID = "ReachValue";

    private static ComponentType<EntityStore, ReachValueQuestListener> listenerType;

    private ReachValueFeature() {}

    /**
     * @return the component naming a player's reach value quests, which holds no meaning outside a
     * registered feature.
     */
    public static ComponentType<EntityStore, ReachValueQuestListener> getListenerType() {
        return listenerType;
    }

    /**
     * Registers the sources shipped with OpenQuests; registering again replaces them.
     */
    public static void registerSources() {
        ValueSource.CODEC.register(GameFlagSource.TYPE, GameFlagSource.class, GameFlagSource.CODEC);
        ValueSource.CODEC.register(MemoriesSource.TYPE, MemoriesSource.class, MemoriesSource.CODEC);
    }

    /**
     * Registers the quest type, its sources, its player listener and the system reading values
     * every tick.
     */
    public static void register(@Nonnull JavaPlugin plugin, @Nonnull OpenQuestsServices services) {
        registerSources();
        services.getQuestProgressionService().registerQuestType(TYPE_ID, ReachValueQuestAsset.class, ReachValueQuestAsset.CODEC, ReachValueQuestProgression.class, ReachValueQuestProgression.CODEC);

        // Codec-less, which keeps it out of the player file: it is built back on connection
        listenerType = plugin.getEntityStoreRegistry().registerComponent(ReachValueQuestListener.class, ReachValueQuestListener::new);
        QuestListenerService.register(ReachValueQuestProgression.class, listenerType);

        plugin.getEntityStoreRegistry().registerSystem(new ReachValueTickingSystem(services.getQuestProgressionService()));
    }
}
