package com.martelstudios.openquests.core.scopes.universe;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.martelstudios.openquests.core.OpenQuestsCorePlugin;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.scopes.ScopeIndexes;
import com.martelstudios.openquests.core.services.QuestProgressionService;

import javax.annotation.Nonnull;
import java.util.Collection;
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

    @Nonnull
    private final ScopeIndexes indexes;

    public UniverseQuestService(@Nonnull JavaPlugin javaPlugin, @Nonnull ScopeIndexes indexes) {
        this.indexes = indexes;
        javaPlugin.getEventRegistry().registerGlobal(PlayerConnectEvent.class, this::handlePlayerConnectEvent);
    }

    public static UniverseQuestService get() {
        return OpenQuestsCorePlugin.get().getUniverseQuestService();
    }

    /**
     * @return the live ids of the running universe quests.
     */
    @Nonnull
    public Set<UUID> getQuestIds() {
        return indexes.getIds(UNIVERSE_INDEX_KEY);
    }

    public void addQuest(@Nonnull UUID questId) {
        AbstractQuestProgression<?> quest = QuestProgressionService.get().loadQuest(questId);
        if (quest == null) return;

        if (!indexes.add(UNIVERSE_INDEX_KEY, questId)) return;

        LOGGER.atInfo().log("Added quest %s to universe", questId);

        if (!(quest.getScope() instanceof UniverseQuestScope)) quest.setScope(UniverseQuestScope.INSTANCE);

        joinEveryone(quest);
    }

    public void removeQuest(@Nonnull UUID questId) {
        LOGGER.atInfo().log("Removing quest %s from universe", questId);

        indexes.remove(UNIVERSE_INDEX_KEY, questId);

        // Gone already when it left for good, and nothing then is left to write on
        AbstractQuestProgression<?> quest = QuestProgressionService.get().getQuest(questId);
        if (quest != null && quest.getScope() instanceof UniverseQuestScope) quest.setScope(null);
    }

    /**
     * Takes an ended quest off the index of running ones. Its players keep it in their journals,
     * and the quest keeps its scope, which says it was the server's.
     */
    public void unindex(@Nonnull UUID questId) {
        indexes.remove(UNIVERSE_INDEX_KEY, questId);
    }

    /**
     * Has everyone online here join the quests another server started.
     */
    public void joinAdded(@Nonnull Collection<UUID> questIds) {
        for (UUID questId : questIds) {
            AbstractQuestProgression<?> quest = QuestProgressionService.get().loadQuest(questId);
            if (quest != null) joinEveryone(quest);
        }
    }

    /**
     * Reads the universe index back and pulls every quest it lists into memory. The index holds
     * running quests only: one that ended is left to its players' journals.
     */
    public void loadQuests() {
        for (AbstractQuestProgression<?> quest : indexes.resolve(UNIVERSE_INDEX_KEY)) {
            if (quest.isCompleted()) indexes.remove(UNIVERSE_INDEX_KEY, quest.getId());
        }
    }

    private static void joinEveryone(@Nonnull AbstractQuestProgression<?> quest) {
        for (PlayerRef playerRef : Universe.get().getPlayers()) {
            QuestProgressionService.get().joinQuest(quest, playerRef.getUuid());
        }
    }

    /**
     * Assigns every universe quest to a connecting player through their incoming holder: they are
     * not online yet, and stored data would be overwritten.
     */
    private void handlePlayerConnectEvent(@Nonnull PlayerConnectEvent playerConnectEvent) {
        var holder = playerConnectEvent.getHolder();
        var playerRef = holder.getComponent(PlayerRef.getComponentType());
        if (playerRef == null) return;

        for (AbstractQuestProgression<?> quest : indexes.resolve(UNIVERSE_INDEX_KEY)) {
            QuestProgressionService.get().joinQuest(quest, playerRef.getUuid());
        }
    }
}
