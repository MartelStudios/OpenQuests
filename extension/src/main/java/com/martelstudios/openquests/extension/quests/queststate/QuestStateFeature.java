package com.martelstudios.openquests.extension.quests.queststate;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.events.QuestLoadedEvent;
import com.martelstudios.openquests.core.events.QuestStateChangedEvent;
import com.martelstudios.openquests.core.events.QuestUnloadedEvent;
import com.martelstudios.openquests.core.scopes.player.events.QuestAddedToPlayerStoreEvent;
import com.martelstudios.openquests.core.scopes.player.events.QuestRemovedFromPlayerStoreEvent;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.extension.journal.QuestPageService;

import javax.annotation.Nonnull;

/**
 * Turns another quest's state into a step of its own.
 */
public final class QuestStateFeature {
    public static final String TYPE_ID = "QuestState";

    private static QuestStateIndex index;

    private QuestStateFeature() {}

    /**
     * @return the index this feature owns, which holds no meaning outside a registered feature.
     */
    @Nonnull
    public static QuestStateIndex getIndex() {
        return index;
    }

    public static void register(@Nonnull JavaPlugin plugin) {
        index = new QuestStateIndex();

        QuestProgressionService.get()
                               .registerQuestType(TYPE_ID, QuestStateQuestAsset.class, QuestStateQuestAsset.CODEC, QuestStateQuestProgression.class, QuestStateQuestProgression.CODEC);

        QuestPageService.register(new QuestStateQuestPageRenderer());

        plugin.getEventRegistry().registerGlobal(QuestLoadedEvent.class, QuestStateQuestEvents::handleQuestLoaded);
        plugin.getEventRegistry().registerGlobal(QuestUnloadedEvent.class, QuestStateQuestEvents::handleQuestUnloaded);
        plugin.getEventRegistry().registerGlobal(QuestAddedToPlayerStoreEvent.class, QuestStateQuestEvents::handleQuestAddedToPlayerStore);
        plugin.getEventRegistry().registerGlobal(QuestRemovedFromPlayerStoreEvent.class, QuestStateQuestEvents::handleQuestRemovedFromPlayerStore);
        plugin.getEventRegistry().registerGlobal(QuestStateChangedEvent.class, QuestStateQuestEvents::handleQuestStateChanged);
    }
}
