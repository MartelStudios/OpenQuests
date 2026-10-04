package com.martelstudios.openquests.core.scopes.universe;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.martelstudios.openquests.core.OpenQuestsCorePlugin;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.persistence.QuestStorage;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.core.stores.QuestsRecord;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

/**
 * Holds the quests shared by every player regardless of world: assigns them to whoever is online
 * and to whoever connects later, and writes on each the {@link UniverseQuestScope} sharing it.
 */
public class UniverseQuestService {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /**
     * The key this scope's index is written under, shared with every server on the same storage.
     */
    public static final String UNIVERSE_INDEX_KEY = "universe";

    private final QuestStorage storage;
    private final QuestsRecord quests = new QuestsRecord();

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

        if (!(quest.getScope() instanceof UniverseQuestScope)) quest.setScope(UniverseQuestScope.INSTANCE);

        for (PlayerRef playerRef : Universe.get().getPlayers()) {
            QuestProgressionService.get().joinQuest(quest, playerRef.getUuid());
        }
    }

    public void removeQuest(@Nonnull UUID questId) {
        LOGGER.atInfo().log("Removing quest %s from universe", questId);

        if (quests.unregister(questId)) dirty = true;

        // Gone already when it left for good, and nothing then is left to write on
        AbstractQuestProgression<?> quest = QuestProgressionService.get().getQuest(questId);
        if (quest != null && quest.getScope() instanceof UniverseQuestScope) quest.setScope(null);
    }

    /**
     * Takes an ended quest off the index of running ones. Its players keep it in their journals,
     * and the quest keeps its scope, which says it was the server's.
     */
    public void unindex(@Nonnull UUID questId) {
        if (quests.unregister(questId)) dirty = true;
    }

    /**
     * Takes in what other servers added to and removed from the index since: a quest one of them
     * started is read back and joined by everyone online here.
     */
    public void refresh(@Nonnull Set<UUID> storedNow) {
        for (UUID questId : quests.absorb(storedNow).added()) {
            AbstractQuestProgression<?> quest = QuestProgressionService.get().loadQuest(questId);
            if (quest == null) continue;

            for (PlayerRef playerRef : Universe.get().getPlayers()) {
                QuestProgressionService.get().joinQuest(quest, playerRef.getUuid());
            }
        }
    }

    /**
     * Reads the universe index back and pulls every quest it lists into memory.
     */
    public void loadQuests() {
        quests.load(storage.loadIndex(UNIVERSE_INDEX_KEY));

        QuestProgressionService.get().loadQuests(quests.getAllIds());

        for (UUID questId : new ArrayList<>(quests.getAllIds())) {
            AbstractQuestProgression<?> quest = QuestProgressionService.get().loadQuest(questId);

            // The index holds running quests only: one that ended is left to its players' journals
            if (quest != null && quest.isCompleted()) {
                quests.unregister(questId);
                dirty = true;
            }
            if (quest != null) continue;

            // Left on the index, so that a quest set aside comes back with its asset
            if (QuestProgressionService.get().isSetAside(questId)) continue;

            quests.unregister(questId);
            dirty = true;
        }
    }

    /**
     * @param force writes the index even if nothing changed, for a shutdown that nothing will
     * follow.
     */
    public void saveQuests(boolean force) {
        if (!dirty && !force) return;

        quests.flush(storage, UNIVERSE_INDEX_KEY);
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
                if (QuestProgressionService.get().isSetAside(questId)) continue;

                quests.unregister(questId);
                dirty = true;
                continue;
            }

            QuestProgressionService.get().joinQuest(quest, playerRef.getUuid());
        }
    }
}
