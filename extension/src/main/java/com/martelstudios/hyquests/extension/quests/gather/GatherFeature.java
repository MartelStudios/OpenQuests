package com.martelstudios.hyquests.extension.quests.gather;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.hyquests.core.events.QuestPlayerAddedEvent;
import com.martelstudios.hyquests.core.services.QuestProgressionService;

import javax.annotation.Nonnull;

/**
 * Collect a quantity of an item, however it was obtained. Everything this quest type needs is
 * registered here, so adding a type never means editing the plugin class.
 */
public final class GatherFeature {
    public static final String TYPE_ID = "Gather";

    private GatherFeature() {}

    public static void register(@Nonnull JavaPlugin plugin) {
        QuestProgressionService.get()
                               .registerQuestType(TYPE_ID, GatherQuestAsset.class, GatherQuestAsset.CODEC, GatherQuest.class, GatherQuest.CODEC);

        plugin.getEntityStoreRegistry().registerSystem(new GatherItemEventSystem());
        plugin.getEventRegistry().registerGlobal(QuestPlayerAddedEvent.class, GatherEvents::handleQuestAssignedToPlayer);
    }
}
