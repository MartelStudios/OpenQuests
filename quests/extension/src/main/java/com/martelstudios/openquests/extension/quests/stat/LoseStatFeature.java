package com.martelstudios.openquests.extension.quests.stat;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.openquests.core.OpenQuestsServices;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.extension.listener.QuestListenerService;

import javax.annotation.Nonnull;

/**
 * Lose a number of points of one of the player's stats: health, stamina, mana, oxygen or one a mod
 * adds.
 */
public final class LoseStatFeature {
    public static final String TYPE_ID = "LoseStat";

    private static ComponentType<EntityStore, LoseStatQuestListener> listenerType;

    private LoseStatFeature() {}

    /**
     * @return the component naming a player's lose stat quests, null before {@link #register}.
     */
    public static ComponentType<EntityStore, LoseStatQuestListener> getListenerType() {
        return listenerType;
    }

    /**
     * Registers the quest type, its listener component and the system counting the losses.
     */
    public static void register(@Nonnull JavaPlugin plugin, @Nonnull OpenQuestsServices services) {
        QuestProgressionService service = services.getQuestProgressionService();

        service.registerQuestType(TYPE_ID, LoseStatQuestAsset.class, LoseStatQuestAsset.CODEC, LoseStatQuestProgression.class, LoseStatQuestProgression.CODEC);

        // Without a codec, so it stays out of the player's file
        listenerType = plugin.getEntityStoreRegistry().registerComponent(LoseStatQuestListener.class, LoseStatQuestListener::new);
        QuestListenerService.register(LoseStatQuestProgression.class, listenerType);

        plugin.getEntityStoreRegistry().registerSystem(new LoseStatTickingSystem(service));
    }
}
