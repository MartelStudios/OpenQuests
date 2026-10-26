package com.martelstudios.openquests.extension.quests.queststate;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.OpenQuestsServices;
import com.martelstudios.openquests.core.events.QuestLoadedEvent;
import com.martelstudios.openquests.core.events.QuestPlayerAbandonedEvent;
import com.martelstudios.openquests.core.events.QuestStateChangedEvent;
import com.martelstudios.openquests.core.events.QuestUnloadedEvent;
import com.martelstudios.openquests.core.scopes.player.events.QuestAddedToPlayerStoreEvent;
import com.martelstudios.openquests.core.scopes.player.events.QuestRemovedFromPlayerStoreEvent;
import com.martelstudios.openquests.extension.journal.QuestPageService;

import javax.annotation.Nonnull;

/**
 * Turns another quest's state into a step of its own.
 */
public final class QuestStateFeature {
    public static final String TYPE_ID = "QuestState";

    private QuestStateFeature() {}

    public static void register(@Nonnull JavaPlugin plugin, @Nonnull OpenQuestsServices services) {
        services.getQuestProgressionService().registerQuestType(TYPE_ID, QuestStateQuestAsset.class, QuestStateQuestAsset.CODEC, QuestStateQuestProgression.class, QuestStateQuestProgression.CODEC);

        QuestPageService.register(new QuestStateQuestPageRenderer());

        QuestStateQuestEvents events = new QuestStateQuestEvents(new QuestStateIndex(), services.getQuestProgressionService());
        plugin.getEventRegistry().registerGlobal(QuestLoadedEvent.class, events::handleQuestLoaded);
        plugin.getEventRegistry().registerGlobal(QuestUnloadedEvent.class, events::handleQuestUnloaded);
        plugin.getEventRegistry().registerGlobal(QuestAddedToPlayerStoreEvent.class, events::handleQuestAddedToPlayerStore);
        plugin.getEventRegistry().registerGlobal(QuestRemovedFromPlayerStoreEvent.class, events::handleQuestRemovedFromPlayerStore);
        plugin.getEventRegistry().registerGlobal(QuestStateChangedEvent.class, events::handleQuestStateChanged);
        plugin.getEventRegistry().registerGlobal(QuestPlayerAbandonedEvent.class, events::handleQuestPlayerAbandoned);
    }
}
