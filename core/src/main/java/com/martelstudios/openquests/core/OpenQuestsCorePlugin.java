package com.martelstudios.openquests.core;

import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.codec.util.RawJsonReader;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import com.hypixel.hytale.server.core.asset.LoadAssetEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.Config;
import com.martelstudios.openquests.core.assignments.OpenQuestAssignment;
import com.martelstudios.openquests.core.assignments.QuestAssignmentService;
import com.martelstudios.openquests.core.assignments.repeat.AfterEndRepeat;
import com.martelstudios.openquests.core.assignments.repeat.AssignmentRepeat;
import com.martelstudios.openquests.core.assignments.repeat.OnceRepeat;
import com.martelstudios.openquests.core.assignments.repeat.ReplaceRepeat;
import com.martelstudios.openquests.core.assignments.scope.AssignmentScope;
import com.martelstudios.openquests.core.assignments.scope.PlayerAssignmentScope;
import com.martelstudios.openquests.core.assignments.scope.UniverseAssignmentScope;
import com.martelstudios.openquests.core.assignments.scope.WorldAssignmentScope;
import com.martelstudios.openquests.core.assignments.scope.WorldsAssignmentScope;
import com.martelstudios.openquests.core.assignments.trigger.AssignmentTrigger;
import com.martelstudios.openquests.core.assignments.trigger.PlayerConnectTrigger;
import com.martelstudios.openquests.core.assignments.trigger.PlayerEnterWorldTrigger;
import com.martelstudios.openquests.core.assignments.trigger.ScheduleTrigger;
import com.martelstudios.openquests.core.commands.QuestCommand;
import com.martelstudios.openquests.core.config.OpenQuestsConfig;
import com.martelstudios.openquests.core.constraints.QuestConstraintValidator;
import com.martelstudios.openquests.core.events.QuestCompletedEvent;
import com.martelstudios.openquests.core.events.QuestUnregisteredEvent;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import com.martelstudios.openquests.core.models.OpenQuestCategory;
import com.martelstudios.openquests.core.models.QuestScope;
import com.martelstudios.openquests.core.persistence.QuestStorage;
import com.martelstudios.openquests.core.persistence.QuestStorageException;
import com.martelstudios.openquests.core.persistence.QuestStorageProvider;
import com.martelstudios.openquests.core.persistence.disk.DiskQuestStorage;
import com.martelstudios.openquests.core.persistence.disk.DiskQuestStorageProvider;
import com.martelstudios.openquests.core.persistence.jdbc.JdbcQuestStorage;
import com.martelstudios.openquests.core.persistence.jdbc.JdbcQuestStorageProvider;
import com.martelstudios.openquests.core.rewards.services.QuestRewardService;
import com.martelstudios.openquests.core.rewards.stores.PendingRewardStoreComponent;
import com.martelstudios.openquests.core.scopes.ScopeIndexes;
import com.martelstudios.openquests.core.scopes.player.PlayerQuestService;
import com.martelstudios.openquests.core.scopes.universe.UniverseQuestScope;
import com.martelstudios.openquests.core.scopes.universe.UniverseQuestService;
import com.martelstudios.openquests.core.scopes.world.WorldQuestScope;
import com.martelstudios.openquests.core.scopes.world.WorldClosingService;
import com.martelstudios.openquests.core.scopes.world.WorldGroupIndex;
import com.martelstudios.openquests.core.scopes.world.WorldQuestService;
import com.martelstudios.openquests.core.scopes.world.WorldsQuestScope;
import com.martelstudios.openquests.core.services.QuestDeadlineService;
import com.martelstudios.openquests.core.services.QuestPlayerStateService;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.core.stores.QuestProgressionStore;
import com.martelstudios.openquests.core.stores.QuestStoreComponent;
import com.martelstudios.openquests.core.sync.QuestSyncService;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * The quest system itself: definitions as assets, per-instance runtime progression, scopes and
 * rewards. Ships no quest type of its own: those are registered on top, by
 * {@code OpenQuestsPlugin} or by any other plugin.
 */
public class OpenQuestsCorePlugin extends JavaPlugin {

    /**
     * Names a config file outside the plugin data directory. The system property wins over the
     * environment variable, and either replaces {@code config.json} whole.
     */
    private static final String CONFIG_PATH_PROPERTY = "openquests.config";

    private static final String CONFIG_PATH_ENVIRONMENT = "OPENQUESTS_CONFIG";

    private static OpenQuestsCorePlugin instance;

    private final Config<OpenQuestsConfig> config;

    private OpenQuestsConfig settings;

    private QuestStorage questStorage;
    private QuestProgressionStore questProgressionStore;
    private ComponentType<EntityStore, QuestStoreComponent> questStoreComponentType;
    private ComponentType<EntityStore, PendingRewardStoreComponent> pendingRewardStoreComponentType;

    private QuestProgressionService questProgressionService;
    private QuestPlayerStateService questPlayerStateService;
    private QuestAssignmentService questAssignmentService;
    private QuestSyncService questSyncService;
    private QuestDeadlineService questDeadlineService;
    private QuestRewardService questRewardService;
    private UniverseQuestService universeQuestService;
    private WorldQuestService worldQuestService;

    private ScopeIndexes scopeIndexes;
    private WorldGroupIndex worldGroupIndex;
    private PlayerQuestService playerQuestService;

    public OpenQuestsCorePlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;

        // Before the config is read, since what it names under "Storage" is resolved through this
        QuestStorageProvider.CODEC.register(DiskQuestStorage.ID, DiskQuestStorageProvider.class, DiskQuestStorageProvider.CODEC);
        QuestStorageProvider.CODEC.register(JdbcQuestStorage.ID, JdbcQuestStorageProvider.class, JdbcQuestStorageProvider.CODEC);

        config = withConfig(OpenQuestsConfig.CODEC);
    }

    public static OpenQuestsCorePlugin get() {
        return instance;
    }

    @Override
    protected void setup() {
        super.setup();

        settings = readSettings();

        getLogger().atInfo().log("Quest storage: %s", settings.getStorage());
        questStorage = settings.getStorage().create();
        questStorage.start();

        questProgressionStore = new QuestProgressionStore(questStorage);
        questSyncService = new QuestSyncService(questStorage, questProgressionStore, settings.getSyncSeconds());

        questProgressionService = new QuestProgressionService(this, questProgressionStore);
        questPlayerStateService = new QuestPlayerStateService(this, questProgressionStore);
        questRewardService = new QuestRewardService(this);
        scopeIndexes = new ScopeIndexes(questStorage, questSyncService);
        universeQuestService = new UniverseQuestService(this, scopeIndexes);
        worldQuestService = new WorldQuestService(this, scopeIndexes);
        worldGroupIndex = new WorldGroupIndex(scopeIndexes);
        new WorldClosingService(this, questStorage);

        QuestScope.CODEC.register(WorldQuestScope.TYPE, WorldQuestScope.class, WorldQuestScope.CODEC);
        QuestScope.CODEC.register(WorldsQuestScope.TYPE, WorldsQuestScope.class, WorldsQuestScope.CODEC);
        QuestScope.CODEC.register(UniverseQuestScope.TYPE, UniverseQuestScope.class, UniverseQuestScope.CODEC);

        // A quest leaving for good leaves whatever shared it, which only its scope knows how to undo
        getEventRegistry().registerGlobal(QuestUnregisteredEvent.class, event -> {
            QuestScope scope = event.getQuest().getScope();
            if (scope != null) scope.release(event.getQuest());
        });

        // Indexes hold running quests only: one ending leaves them, its players keeping it in their
        // journals, and one nobody holds any more is done away with, nothing being left to read it
        getEventRegistry().registerGlobal(QuestCompletedEvent.class, event -> {
            AbstractQuestProgression<?> quest = event.getQuest();
            if (!quest.isStopOnComplete()) return;

            QuestScope scope = quest.getScope();
            if (scope != null) scope.retire(quest);

            if (quest.getPlayers().isEmpty() && quest.getAbandonedPlayers().isEmpty()) questProgressionService.unregisterQuest(quest);
        });

        playerQuestService = new PlayerQuestService(this);
        questAssignmentService = new QuestAssignmentService(this, questStorage);

        AssignmentTrigger.CODEC.register(PlayerConnectTrigger.TYPE, PlayerConnectTrigger.class, PlayerConnectTrigger.CODEC);
        AssignmentTrigger.CODEC.register(PlayerEnterWorldTrigger.TYPE, PlayerEnterWorldTrigger.class, PlayerEnterWorldTrigger.CODEC);
        AssignmentTrigger.CODEC.register(ScheduleTrigger.TYPE, ScheduleTrigger.class, ScheduleTrigger.CODEC);
        AssignmentScope.CODEC.register(PlayerAssignmentScope.TYPE, PlayerAssignmentScope.class, PlayerAssignmentScope.CODEC);
        AssignmentScope.CODEC.register(WorldAssignmentScope.TYPE, WorldAssignmentScope.class, WorldAssignmentScope.CODEC);
        AssignmentScope.CODEC.register(WorldsAssignmentScope.TYPE, WorldsAssignmentScope.class, WorldsAssignmentScope.CODEC);
        AssignmentScope.CODEC.register(UniverseAssignmentScope.TYPE, UniverseAssignmentScope.class, UniverseAssignmentScope.CODEC);
        AssignmentRepeat.CODEC.register(OnceRepeat.TYPE, OnceRepeat.class, OnceRepeat.CODEC);
        AssignmentRepeat.CODEC.register(AfterEndRepeat.TYPE, AfterEndRepeat.class, AfterEndRepeat.CODEC);
        AssignmentRepeat.CODEC.register(ReplaceRepeat.TYPE, ReplaceRepeat.class, ReplaceRepeat.CODEC);
        questDeadlineService = new QuestDeadlineService(this);

        // Both indexes are rebuilt from the storage on connection and on world entry, so neither
        // is written to the entity files the server saves for us
        questStoreComponentType = getEntityStoreRegistry().registerComponent(QuestStoreComponent.class, QuestStoreComponent::new);
        pendingRewardStoreComponentType = getEntityStoreRegistry().registerComponent(PendingRewardStoreComponent.class, PendingRewardStoreComponent::new);

        getCommandRegistry().registerCommand(new QuestCommand());

        getAssetRegistry().register(HytaleAssetStore.builder(OpenQuestCategory.class, new DefaultAssetMap<>())
                                                    .setPath("OpenQuests/Categories/")
                                                    .setCodec(OpenQuestCategory.CODEC)
                                                    .setKeyFunction(OpenQuestCategory::getId)
                                                    .build());

        // After the categories, which the quests name and are checked against
        getAssetRegistry().register(HytaleAssetStore.builder(OpenQuestAsset.class, new DefaultAssetMap<>())
                                                    .setPath("OpenQuests/Quests/")
                                                    .setCodec(OpenQuestAsset.CODEC)
                                                    .setKeyFunction(OpenQuestAsset::getId)
                                                    .loadsAfter(OpenQuestCategory.class)
                                                    .build());

        // After the quests, which the assignments name and are checked against
        getAssetRegistry().register(HytaleAssetStore.builder(OpenQuestAssignment.class, new DefaultAssetMap<>())
                                                    .setPath("OpenQuests/Assignments/")
                                                    .setCodec(OpenQuestAssignment.CODEC)
                                                    .setKeyFunction(OpenQuestAssignment::getId)
                                                    .loadsAfter(OpenQuestAsset.class)
                                                    .build());

        getEventRegistry().registerGlobal(LoadAssetEvent.PRIORITY_LOAD_LATE, LoadAssetEvent.class, QuestConstraintValidator::handleLoadAsset);
    }

    @Override
    protected void start() {
        universeQuestService.loadQuests();
        worldGroupIndex.preload(groupsOfAssignments());
        questAssignmentService.start();

        long interval = settings.getSaveIntervalMinutes();

        HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(() -> saveEverything(false), interval, interval, TimeUnit.MINUTES);

        questSyncService.start();
    }

    /**
     * @return the groups of worlds the assignments gather, read back as the server starts.
     */
    @Nonnull
    private Set<String> groupsOfAssignments() {
        Set<String> groups = new HashSet<>();
        for (OpenQuestAssignment assignment : OpenQuestAssignment.getAssetMap().getAssetMap().values()) {
            if (assignment.getScope() instanceof WorldsAssignmentScope) groups.add(assignment.getId());
        }
        return groups;
    }

    @Override
    protected void shutdown() {
        saveEverything(true);

        // After the last pass, which hands the shared quests to the storage thread to write
        if (questSyncService != null) questSyncService.stop();

        if (questStorage != null) questStorage.close();
    }

    /**
     * The quests first, then the indexes naming them: an index written before the quest it points
     * at would name one that is not there yet if the server stopped in between.
     */
    private void saveEverything(boolean force) {
        save("quests", questProgressionStore::saveAll);
        save("player records", () -> questPlayerStateService.saveAllOnline(force));
        save("the quest indexes", scopeIndexes::flushAll);
    }

    /**
     * One step of the pass, on its own, so that a step that throws does not take the rest with it.
     */
    private void save(@Nonnull String what, @Nonnull Runnable step) {
        try {
            step.run();
        } catch (RuntimeException e) {
            getLogger().atWarning().withCause(e).log("Failed to save %s", what);
        }
    }

    /**
     * The config, from {@code config.json} unless something names another file. An override is
     * needed where the data directory is no place to leave one: the Gradle workspace re-links it
     * on every {@code runAllMods}, and a password has no business in the mods directory.
     */
    @Nonnull
    private OpenQuestsConfig readSettings() {
        String override = System.getProperty(CONFIG_PATH_PROPERTY);
        if (override == null || override.isBlank()) override = System.getenv(CONFIG_PATH_ENVIRONMENT);

        if (override == null || override.isBlank()) {
            writeDefaultConfig();
            return config.get();
        }

        Path path = Paths.get(override).toAbsolutePath();
        try {
            OpenQuestsConfig read = RawJsonReader.readSync(path, OpenQuestsConfig.CODEC, getLogger());
            if (read == null) throw new IOException("the file is empty");

            getLogger().atInfo().log("Quest config read from %s", path);
            return read;
        } catch (IOException e) {
            // A server told to use a database and quietly given files looks fine until it restarts
            throw new QuestStorageException("Failed to read the quest config at " + path, e);
        }
    }

    /**
     * Writes the defaults out the first time, so a server owner has the shape of the file in front
     * of them rather than a page of documentation.
     */
    private void writeDefaultConfig() {
        if (Files.exists(getDataDirectory().resolve("config.json"))) return;

        config.save();
    }

    @Nonnull
    public QuestStorage getQuestStorage() {
        return questStorage;
    }

    public QuestProgressionStore getQuestProgressionStore() {
        return questProgressionStore;
    }

    public UniverseQuestService getUniverseQuestService() {
        return universeQuestService;
    }

    public WorldQuestService getWorldQuestService() {
        return worldQuestService;
    }

    public ScopeIndexes getScopeIndexes() {
        return scopeIndexes;
    }

    public WorldGroupIndex getWorldGroupIndex() {
        return worldGroupIndex;
    }

    public PlayerQuestService getPlayerQuestService() {
        return playerQuestService;
    }

    public QuestPlayerStateService getQuestPlayerStateService() {
        return questPlayerStateService;
    }

    public QuestAssignmentService getQuestAssignmentService() {
        return questAssignmentService;
    }

    public QuestSyncService getQuestSyncService() {
        return questSyncService;
    }

    public QuestDeadlineService getQuestDeadlineService() {
        return questDeadlineService;
    }

    public QuestProgressionService getQuestProgressionService() {
        return questProgressionService;
    }

    public QuestRewardService getQuestRewardService() {
        return questRewardService;
    }

    public ComponentType<EntityStore, QuestStoreComponent> getQuestStoreComponentType() {
        return questStoreComponentType;
    }

    public ComponentType<EntityStore, PendingRewardStoreComponent> getPendingRewardStoreComponentType() {
        return pendingRewardStoreComponentType;
    }
}
