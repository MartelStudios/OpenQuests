package com.martelstudios.openquests.core.scopes.universe;

import com.hypixel.hytale.event.EventRegistration;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.martelstudios.openquests.core.OpenQuestsCorePlugin;
import com.martelstudios.openquests.core.events.QuestUnregisteredEvent;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.persistence.QuestStorage;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.core.stores.QuestsRecord;

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

    /**
     * The key this scope's index is written under, shared with every server on the same storage.
     */
    public static final String UNIVERSE_INDEX_KEY = "universe";

    private final QuestStorage storage;
    private final QuestsRecord quests = new QuestsRecord();
    private final ConcurrentHashMap<UUID, EventRegistration<UUID, QuestUnregisteredEvent>> questUnregisteredListeners = new ConcurrentHashMap<>();

    private boolean dirty;

    public UniverseQuestService(@Nonnull JavaPlugin javaPlugin, @Nonnull QuestStorage storage) {
        this.storage = storage;
        javaPlugin.getEventRegistry().registerGlobal(PlayerConnectEvent.class, this::handlePlayerConnectEvent);
    }

    public static UniverseQuestService get() {
        return OpenQuestsCorePlugin.get().getUniverseQuestService();
    }

    /**
     * @return the live index of universe quests. Mutating it is what gets persisted on the next
     * pass, so a caller adding an id here should say so through {@link #addQuest}.
     */
    @Nonnull
    public QuestsRecord getQuests() {
        return quests;
    }

    public void addQuest(@Nonnull UUID questId) {
        AbstractQuestProgression<?> quest = QuestProgressionService.get().loadQuest(questId);
        if (quest == null) return;

        if (!quests.register(questId)) return;
        dirty = true;

        LOGGER.atInfo().log("Added quest %s to universe", questId);

        trackQuest(questId);

        for (PlayerRef playerRef : Universe.get().getPlayers()) {
            QuestProgressionService.get().joinQuest(quest, playerRef.getUuid());
        }
    }

    public void removeQuest(@Nonnull UUID questId) {
        LOGGER.atInfo().log("Removing quest %s from universe", questId);

        if (quests.unregister(questId)) dirty = true;
        untrackQuest(questId);
    }

    /**
     * Reads the universe index back and pulls every quest it lists into memory. Re-arms the
     * tracking, without which a quest completed after a restart would never leave the index.
     */
    public void loadQuests() {
        quests.replaceAll(storage.loadIndex(UNIVERSE_INDEX_KEY));

        for (UUID questId : new ArrayList<>(quests.getAllIds())) {
            if (QuestProgressionService.get().loadQuest(questId) == null) {
                quests.unregister(questId);
                dirty = true;
                continue;
            }

            trackQuest(questId);
        }
    }

    /**
     * @param force writes the index even if nothing changed, for a shutdown that nothing will
     * follow.
     */
    public void saveQuests(boolean force) {
        if (!dirty && !force) return;

        storage.saveIndex(UNIVERSE_INDEX_KEY, quests.getAllIds());
        dirty = false;
    }

    /**
     * Assigns every universe quest to a connecting player through their incoming holder: they are
     * not online yet, and stored data would be overwritten.
     */
    private void handlePlayerConnectEvent(@Nonnull PlayerConnectEvent playerConnectEvent) {
        var holder = playerConnectEvent.getHolder();
        var playerRef = holder.getComponent(PlayerRef.getComponentType());
        if (playerRef == null) return;

        for (UUID questId : new ArrayList<>(quests.getAllIds())) {
            AbstractQuestProgression<?> quest = QuestProgressionService.get().loadQuest(questId);
            if (quest == null) {
                quests.unregister(questId);
                dirty = true;
                continue;
            }

            QuestProgressionService.get().joinQuest(quest, playerRef.getUuid());
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
