package com.martelstudios.hyquests.core.services;

import com.hypixel.hytale.event.EventRegistration;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.martelstudios.hyquests.core.HyQuestCorePlugin;
import com.martelstudios.hyquests.core.events.QuestUnregisteredEvent;
import com.martelstudios.hyquests.core.models.AbstractQuest;
import com.martelstudios.hyquests.core.stores.QuestsRecord;
import com.martelstudios.hyquests.core.stores.QuestsStore;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds the quests shared by every player regardless of world. Quests know nothing about this
 * scope: the service assigns them to whoever is online, and to whoever connects later.
 */
public class UniverseQuestService {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static final String UNIVERSE_QUEST_INDEX_KEY = "universe";

    private final QuestsStore questsStore;
    private final ConcurrentHashMap<UUID, EventRegistration<UUID, QuestUnregisteredEvent>> questUnregisteredListeners = new ConcurrentHashMap<>();

    public UniverseQuestService(QuestsStore questsStore) {
        this.questsStore = questsStore;
    }

    public static UniverseQuestService get() {
        return HyQuestCorePlugin.get().getUniverseQuestService();
    }

    /**
     * @return the live index of universe quests, owned by the store so that mutating it is
     * exactly what gets persisted.
     */
    @Nonnull
    public QuestsRecord getQuests() {
        return questsStore.get(UNIVERSE_QUEST_INDEX_KEY);
    }

    public void addQuest(@Nonnull UUID questId) {
        AbstractQuest<?> quest = QuestProgressionService.get().getQuest(questId);
        if (quest == null) return;

        if (!getQuests().register(questId)) return;

        LOGGER.atInfo().log("Added quest %s to universe", questId);

        trackQuest(questId);

        for (PlayerRef playerRef : Universe.get().getPlayers()) {
            quest.addPlayer(playerRef.getUuid());
        }
    }

    public void removeQuest(@Nonnull UUID questId) {
        LOGGER.atInfo().log("Removing quest %s from universe", questId);

        getQuests().unregister(questId);
        untrackQuest(questId);
    }

    /**
     * Loads the universe-scope quest index and pulls every quest it lists into the datastore.
     * Re-arms the tracking, without which a quest completed after a restart would leave its id
     * in the index forever.
     */
    public void loadQuests() {
        QuestsRecord quests = questsStore.load(UNIVERSE_QUEST_INDEX_KEY);

        for (UUID questId : new ArrayList<>(quests.getAllIds())) {
            if (QuestProgressionService.get().loadQuest(questId) == null) {
                quests.unregister(questId);
                continue;
            }

            trackQuest(questId);
        }
    }

    public void saveUniverseQuestIndex() {
        questsStore.save(UNIVERSE_QUEST_INDEX_KEY);
    }

    /**
     * Assigns every universe quest to a connecting player, through their incoming holder rather
     * than their id: they are not online yet, and stored data would be overwritten.
     */
    public void handlePlayerConnectEvent(@Nonnull PlayerConnectEvent playerConnectEvent) {
        var holder = playerConnectEvent.getHolder();
        var playerRef = holder.getComponent(PlayerRef.getComponentType());
        if (playerRef == null) return;

        QuestsRecord quests = getQuests();
        for (UUID questId : new ArrayList<>(quests.getAllIds())) {
            AbstractQuest<?> quest = QuestProgressionService.get().getQuest(questId);
            if (quest == null) {
                quests.unregister(questId);
                continue;
            }

            quest.addPlayer(playerRef.getUuid());
        }
    }

    private void handleQuestUnregisteredEvent(QuestUnregisteredEvent questUnregisteredEvent) {
        removeQuest(questUnregisteredEvent.getQuest().getId());
    }

    /**
     * One listener per quest, however many times it is added.
     */
    private void trackQuest(UUID questId) {
        questUnregisteredListeners.computeIfAbsent(questId, id -> HytaleServer.get()
                                                                              .getEventBus()
                                                                              .register(QuestUnregisteredEvent.class, id, this::handleQuestUnregisteredEvent));
    }

    private void untrackQuest(UUID questId) {
        var questListener = questUnregisteredListeners.remove(questId);
        if (questListener != null) {
            questListener.unregister();
        }
    }
}
