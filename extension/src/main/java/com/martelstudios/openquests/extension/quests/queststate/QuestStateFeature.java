package com.martelstudios.openquests.extension.quests.queststate;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.events.QuestCompletedEvent;
import com.martelstudios.openquests.core.events.QuestUpdatedEvent;
import com.martelstudios.openquests.core.scopes.player.events.QuestAddedToPlayerStoreEvent;
import com.martelstudios.openquests.core.services.QuestProgressionService;

import javax.annotation.Nonnull;

/**
 * Turns another quest's state into a step of its own.
 */
public final class QuestStateFeature {
    public static final String TYPE_ID = "QuestState";

    private QuestStateFeature() {}

    public static void register(@Nonnull JavaPlugin plugin) {
        QuestProgressionService.get()
                               .registerQuestType(TYPE_ID, QuestStateQuestAsset.class, QuestStateQuestAsset.CODEC, QuestStateQuestProgression.class, QuestStateQuestProgression.CODEC);

        plugin.getEventRegistry().registerGlobal(QuestAddedToPlayerStoreEvent.class, QuestStateQuestEvents::handleQuestAddedToPlayerStore);
        plugin.getEventRegistry().registerGlobal(QuestUpdatedEvent.class, QuestStateQuestEvents::handleQuestUpdated);
        plugin.getEventRegistry().registerGlobal(QuestCompletedEvent.class, QuestStateQuestEvents::handleQuestCompleted);
    }
}
