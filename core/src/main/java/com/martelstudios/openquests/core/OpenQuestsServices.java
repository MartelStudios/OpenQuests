package com.martelstudios.openquests.core;

import com.hypixel.hytale.event.IEventBus;
import com.hypixel.hytale.event.IEventRegistry;
import com.martelstudios.openquests.core.assignments.QuestAssignmentService;
import com.martelstudios.openquests.core.events.QuestCompletedEvent;
import com.martelstudios.openquests.core.events.QuestUnregisteredEvent;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.QuestEnvironment;
import com.martelstudios.openquests.core.models.QuestScope;
import com.martelstudios.openquests.core.persistence.QuestStorage;
import com.martelstudios.openquests.core.rewards.services.QuestRewardService;
import com.martelstudios.openquests.core.scopes.ScopeIndexFollower;
import com.martelstudios.openquests.core.scopes.ScopeIndexes;
import com.martelstudios.openquests.core.scopes.ScopeServices;
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
import java.time.Clock;

/**
 * The core services, built once over a storage and wired to each other. The plugin builds one
 * against the server, a test one against stand-ins.
 */
public final class OpenQuestsServices implements QuestEnvironment {

    @Nullable
    private static volatile OpenQuestsServices installed;

    private final QuestStorage storage;
    private final Platform platform;
    private final QuestProgressionStore progressionStore;
    private final QuestRewardService rewards;
    private final QuestPlayerStateService playerState;
    private final QuestProgressionService progression;
    private final QuestSyncService sync;
    private final ScopeIndexes scopeIndexes;
    private final PlayerQuestService playerQuests;
    private final UniverseQuestService universe;
    private final WorldQuestService worlds;
    private final WorldGroupIndex worldGroups;
    private final ScopeServices scopes;
    private final WorldClosingService worldClosing;
    private final QuestAssignmentService assignments;
    private final QuestDeadlineService deadlines;

    /**
     * @param syncSeconds how often quests shared between servers are read back
     */
    public OpenQuestsServices(@Nonnull QuestStorage storage, long syncSeconds, @Nonnull Platform platform) {
        this.storage = storage;
        this.platform = platform;

        progressionStore = new QuestProgressionStore(storage, this);
        rewards = new QuestRewardService(this, storage, platform.players(), platform.components());
        playerState = new QuestPlayerStateService(progressionStore, rewards, platform.players(), platform.components());
        progression = new QuestProgressionService(progressionStore, platform.events(), platform.clock(), playerState);
        sync = new QuestSyncService(storage, progressionStore, progression, syncSeconds);
        scopeIndexes = new ScopeIndexes(storage, sync, progressionStore);
        playerQuests = new PlayerQuestService(platform.events(), platform.players(), platform.components());
        universe = new UniverseQuestService(scopeIndexes, progression, platform.players());
        worlds = new WorldQuestService(scopeIndexes, progression, sync, playerQuests, platform.components());
        worldGroups = new WorldGroupIndex(scopeIndexes, progression, worlds);
        scopes = new ScopeServices(universe, worlds, worldGroups);
        worldClosing = new WorldClosingService(storage, progression, scopes, platform.players());
        assignments = new QuestAssignmentService(storage, progression, scopes, platform.players(), platform.components(), platform.clock(), platform.scheduler());
        deadlines = new QuestDeadlineService(progression, platform.clock(), platform.scheduler());

        // What other servers write besides the quests themselves, read on each tick of the sync
        ScopeIndexFollower indexFollower = new ScopeIndexFollower(scopeIndexes, universe, worldGroups);
        sync.onEachTick(indexFollower::follow);
        sync.onEachTick(playerState::takeInMessages);
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
            if (scope != null) scope.release(scopes, event.getQuest());
        });

        // Indexes hold running quests only: one ending leaves them, its players keeping it in their
        // journals, and one nobody holds any more is done away with, nothing being left to read it
        events.registerGlobal(QuestCompletedEvent.class, event -> {
            AbstractQuestProgression<?> quest = event.getQuest();
            if (!quest.isStopOnComplete()) return;

            QuestScope scope = quest.getScope();
            if (scope != null) scope.retire(scopes, quest);

            if (quest.getPlayers().isEmpty() && quest.getAbandonedPlayers().isEmpty()) progression.unregisterQuest(quest);
        });

        playerQuests.listen(events);
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
    public Platform getPlatform() {
        return platform;
    }

    @Nonnull
    @Override
    public IEventBus events() {
        return platform.events();
    }

    @Nonnull
    @Override
    public Clock clock() {
        return platform.clock();
    }

    @Nonnull
    @Override
    public OpenQuestsServices services() {
        return this;
    }

    @Override
    public boolean isShared(@Nonnull AbstractQuestProgression<?> quest) {
        return sync.isShared(quest);
    }

    @Override
    public void writeSoon(@Nonnull AbstractQuestProgression<?> quest) {
        sync.writeSoon(quest);
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
    public ScopeServices getScopeServices() {
        return scopes;
    }

    @Nonnull
    public PlayerQuestService getPlayerQuestService() {
        return playerQuests;
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
