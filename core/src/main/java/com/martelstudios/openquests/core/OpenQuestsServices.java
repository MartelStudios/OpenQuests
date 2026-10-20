package com.martelstudios.openquests.core;

import com.hypixel.hytale.event.IEventBus;
import com.hypixel.hytale.event.IEventRegistry;
import com.martelstudios.openquests.core.assignments.QuestAssignmentService;
import com.martelstudios.openquests.core.events.QuestCompletedEvent;
import com.martelstudios.openquests.core.events.QuestUnregisteredEvent;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.QuestScope;
import com.martelstudios.openquests.core.persistence.QuestStorage;
import com.martelstudios.openquests.core.rewards.services.QuestRewardService;
import com.martelstudios.openquests.core.scopes.ScopeIndexes;
import com.martelstudios.openquests.core.scopes.player.PlayerQuestService;
import com.martelstudios.openquests.core.scopes.universe.UniverseQuestService;
import com.martelstudios.openquests.core.scopes.world.WorldClosingService;
import com.martelstudios.openquests.core.scopes.world.WorldGroupIndex;
import com.martelstudios.openquests.core.scopes.world.WorldQuestService;
import com.martelstudios.openquests.core.services.QuestDeadlineService;
import com.martelstudios.openquests.core.services.QuestPlayerStateService;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.core.stores.QuestProgressionStore;
import com.martelstudios.openquests.core.sync.QuestSyncService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The core services, built once over a storage and wired to each other. The plugin builds one
 * against the server, a test one against stand-ins.
 */
public final class OpenQuestsServices {

    @Nullable
    private static volatile OpenQuestsServices installed;

    private final QuestStorage storage;
    private final IEventBus events;
    private final QuestProgressionStore progressionStore;
    private final QuestSyncService sync;
    private final QuestProgressionService progression;
    private final QuestPlayerStateService playerState;
    private final QuestRewardService rewards;
    private final ScopeIndexes scopeIndexes;
    private final UniverseQuestService universe;
    private final WorldQuestService worlds;
    private final WorldGroupIndex worldGroups;
    private final WorldClosingService worldClosing;
    private final PlayerQuestService players;
    private final QuestAssignmentService assignments;
    private final QuestDeadlineService deadlines;

    /**
     * @param syncSeconds how often quests shared between servers are read back
     * @param events where the services announce what happens to quests
     */
    public OpenQuestsServices(@Nonnull QuestStorage storage, long syncSeconds, @Nonnull IEventBus events) {
        this.storage = storage;
        this.events = events;
        progressionStore = new QuestProgressionStore(storage, events);
        sync = new QuestSyncService(storage, progressionStore, syncSeconds);
        progression = new QuestProgressionService(progressionStore, events);
        playerState = new QuestPlayerStateService(progressionStore);
        rewards = new QuestRewardService();
        scopeIndexes = new ScopeIndexes(storage, sync);
        universe = new UniverseQuestService(scopeIndexes);
        worlds = new WorldQuestService(scopeIndexes);
        worldGroups = new WorldGroupIndex(scopeIndexes);
        worldClosing = new WorldClosingService(storage);
        players = new PlayerQuestService(events);
        assignments = new QuestAssignmentService(storage);
        deadlines = new QuestDeadlineService();
    }

    /**
     * Subscribes every service to the events it answers. Listeners of equal priority run in the
     * order they were registered in, which this order keeps.
     */
    public void listen(@Nonnull IEventRegistry events) {
        progression.listen(events);
        playerState.listen(events);
        rewards.listen(events);
        universe.listen(events);
        worlds.listen(events);
        worldClosing.listen(events);

        // A quest leaving for good leaves whatever shared it, which only its scope knows how to undo
        events.registerGlobal(QuestUnregisteredEvent.class, event -> {
            QuestScope scope = event.getQuest().getScope();
            if (scope != null) scope.release(event.getQuest());
        });

        // Indexes hold running quests only: one ending leaves them, its players keeping it in their
        // journals, and one nobody holds any more is done away with, nothing being left to read it
        events.registerGlobal(QuestCompletedEvent.class, event -> {
            AbstractQuestProgression<?> quest = event.getQuest();
            if (!quest.isStopOnComplete()) return;

            QuestScope scope = quest.getScope();
            if (scope != null) scope.retire(quest);

            if (quest.getPlayers().isEmpty() && quest.getAbandonedPlayers().isEmpty()) progression.unregisterQuest(quest);
        });

        players.listen(events);
        assignments.listen(events);
        deadlines.listen(events);
    }

    /**
     * Makes these the services that code not handed them yet finds: the static lookups and the
     * quests built by codecs. The plugin installs its own, a test the one it drives.
     */
    public void install() {
        installed = this;
    }

    /**
     * @return the installed services.
     * @throws IllegalStateException before any were installed, which outside a test means before
     * the plugin set up
     */
    @Nonnull
    public static OpenQuestsServices installed() {
        OpenQuestsServices services = installed;
        if (services == null) throw new IllegalStateException("The OpenQuests services are not set up yet");
        return services;
    }

    @Nonnull
    public IEventBus getEvents() {
        return events;
    }

    @Nonnull
    public QuestStorage getQuestStorage() {
        return storage;
    }

    @Nonnull
    public QuestProgressionStore getQuestProgressionStore() {
        return progressionStore;
    }

    @Nonnull
    public QuestSyncService getQuestSyncService() {
        return sync;
    }

    @Nonnull
    public QuestProgressionService getQuestProgressionService() {
        return progression;
    }

    @Nonnull
    public QuestPlayerStateService getQuestPlayerStateService() {
        return playerState;
    }

    @Nonnull
    public QuestRewardService getQuestRewardService() {
        return rewards;
    }

    @Nonnull
    public ScopeIndexes getScopeIndexes() {
        return scopeIndexes;
    }

    @Nonnull
    public UniverseQuestService getUniverseQuestService() {
        return universe;
    }

    @Nonnull
    public WorldQuestService getWorldQuestService() {
        return worlds;
    }

    @Nonnull
    public WorldGroupIndex getWorldGroupIndex() {
        return worldGroups;
    }

    @Nonnull
    public PlayerQuestService getPlayerQuestService() {
        return players;
    }

    @Nonnull
    public QuestAssignmentService getQuestAssignmentService() {
        return assignments;
    }

    @Nonnull
    public QuestDeadlineService getQuestDeadlineService() {
        return deadlines;
    }
}
