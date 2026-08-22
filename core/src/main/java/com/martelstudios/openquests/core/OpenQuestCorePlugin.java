package com.martelstudios.openquests.core;

import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.ResourceType;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.RemovedPlayerFromWorldEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.datastore.DiskDataStoreProvider;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.openquests.core.assets.QuestAsset;
import com.martelstudios.openquests.core.assignment.assets.QuestAssignmentAsset;
import com.martelstudios.openquests.core.assignment.services.QuestAssignmentService;
import com.martelstudios.openquests.core.assignment.services.QuestAutoAssignmentService;
import com.martelstudios.openquests.core.assignment.stores.QuestAssignmentRecord;
import com.martelstudios.openquests.core.assignment.stores.QuestAssignmentStore;
import com.martelstudios.openquests.core.assignment.stores.QuestAssignmentStoreComponent;
import com.martelstudios.openquests.core.commands.QuestCommand;
import com.martelstudios.openquests.core.events.QuestCompletedEvent;
import com.martelstudios.openquests.core.events.QuestPlayerAddedEvent;
import com.martelstudios.openquests.core.events.QuestPlayerRemovedEvent;
import com.martelstudios.openquests.core.events.QuestUnregisteredEvent;
import com.martelstudios.openquests.core.history.services.QuestHistoryService;
import com.martelstudios.openquests.core.history.stores.QuestHistoryStoreComponent;
import com.martelstudios.openquests.core.scopes.player.PlayerQuestService;
import com.martelstudios.openquests.core.scopes.universe.QuestsStore;
import com.martelstudios.openquests.core.scopes.universe.UniverseQuestService;
import com.martelstudios.openquests.core.scopes.world.WorldQuestService;
import com.martelstudios.openquests.core.scopes.world.WorldQuestStoreResource;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.core.stores.QuestProgressionRecord;
import com.martelstudios.openquests.core.stores.QuestProgressionStore;
import com.martelstudios.openquests.core.stores.QuestStoreComponent;
import com.martelstudios.openquests.core.stores.QuestsRecord;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

/**
 * The quest system itself: definitions as assets, per-instance runtime progression, scopes and
 * rewards. Ships no quest type of its own — those are registered on top, by
 * {@code OpenQuestExtensionPlugin} or by any other plugin.
 */
public class OpenQuestCorePlugin extends JavaPlugin {
    public static final Path questProgressionsPath = Paths.get("quests", "progressions");
    public static final Path questSetStorePath = Paths.get("quests", "stores");
    public static final Path questAssignmentsPath = Paths.get("quests", "assignments");

    private static final long SAVE_INTERVAL_MINUTES = 5;

    private static OpenQuestCorePlugin instance;

    private QuestProgressionStore questProgressionStore;
    private QuestsStore questsStore;
    private ComponentType<EntityStore, QuestStoreComponent> questStoreComponentType;
    private ResourceType<EntityStore, WorldQuestStoreResource> worldStoreResourceType;
    private ComponentType<EntityStore, QuestHistoryStoreComponent> questHistoryStoreComponentType;
    private ComponentType<EntityStore, QuestAssignmentStoreComponent> questAssignmentStoreComponentType;
    private QuestAssignmentStore questAssignmentStore;

    private QuestProgressionService questProgressionService;
    private QuestHistoryService questHistoryService;
    private QuestAssignmentService questAssignmentService;
    private QuestAutoAssignmentService questAutoAssignmentService;
    private UniverseQuestService universeQuestService;
    private WorldQuestService worldQuestService;
    private PlayerQuestService playerQuestService;

    public OpenQuestCorePlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
    }

    public static OpenQuestCorePlugin get() {
        return instance;
    }

    @Override
    protected void setup() {
        super.setup();
        questProgressionStore = new QuestProgressionStore(new DiskDataStoreProvider(questProgressionsPath.toString()).create(QuestProgressionRecord.CODEC));
        questsStore = new QuestsStore(new DiskDataStoreProvider(questSetStorePath.toString()).create(QuestsRecord.CODEC));

        questProgressionService = new QuestProgressionService(questProgressionStore);
        questHistoryService = new QuestHistoryService(this);
        questAssignmentStore = new QuestAssignmentStore(new DiskDataStoreProvider(questAssignmentsPath.toString()).create(QuestAssignmentRecord.CODEC));
        questAssignmentService = new QuestAssignmentService(questAssignmentStore);
        questAutoAssignmentService = new QuestAutoAssignmentService(this);
        universeQuestService = new UniverseQuestService(this, questsStore);
        worldQuestService = new WorldQuestService(this);
        playerQuestService = new PlayerQuestService(this);

        questStoreComponentType = getEntityStoreRegistry().registerComponent(QuestStoreComponent.class, "QuestStore", QuestStoreComponent.CODEC);
        worldStoreResourceType = getEntityStoreRegistry().registerResource(WorldQuestStoreResource.class, "QuestStore", WorldQuestStoreResource.CODEC);
        questHistoryStoreComponentType = getEntityStoreRegistry().registerComponent(QuestHistoryStoreComponent.class, "QuestHistoryStore", QuestHistoryStoreComponent.CODEC);
        questAssignmentStoreComponentType = getEntityStoreRegistry().registerComponent(QuestAssignmentStoreComponent.class, "QuestAssignmentStore", QuestAssignmentStoreComponent.CODEC);

        getCommandRegistry().registerCommand(new QuestCommand());

        getAssetRegistry().register(HytaleAssetStore.builder(QuestAsset.class, new DefaultAssetMap<>())
                                                    .setPath("OpenQuests/Quests/")
                                                    .setCodec(QuestAsset.CODEC)
                                                    .setKeyFunction(QuestAsset::getId)
                                                    .build());

        getAssetRegistry().register(HytaleAssetStore.builder(QuestAssignmentAsset.class, new DefaultAssetMap<>())
                                                    .setPath("OpenQuests/QuestAssignments/")
                                                    .setCodec(QuestAssignmentAsset.CODEC)
                                                    .setKeyFunction(QuestAssignmentAsset::getId)
                                                    .build());
    }

    @Override
    protected void start() {
        universeQuestService.loadQuests();
        questAssignmentService.registerAllAssignmentsToItsConditions();

        HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(() -> {
            questProgressionStore.saveAllToDisk();
            questAssignmentStore.saveAllToDisk();
            universeQuestService.saveUniverseQuestIndex();
        }, SAVE_INTERVAL_MINUTES, SAVE_INTERVAL_MINUTES, TimeUnit.MINUTES);
    }

    @Override
    protected void shutdown() {
        questProgressionStore.saveAllToDisk();
        questAssignmentStore.saveAllToDisk();
        universeQuestService.saveUniverseQuestIndex();
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

    public PlayerQuestService getPlayerQuestService() {
        return playerQuestService;
    }

    public QuestProgressionService getQuestProgressionService() {
        return questProgressionService;
    }

    public QuestHistoryService getQuestHistoryService() {
        return questHistoryService;
    }

    public ComponentType<EntityStore, QuestStoreComponent> getQuestStoreComponentType() {
        return questStoreComponentType;
    }

    public ResourceType<EntityStore, WorldQuestStoreResource> getWorldStoreResourceType() {
        return worldStoreResourceType;
    }

    public QuestAssignmentService getQuestAssignmentService() {
        return questAssignmentService;
    }

    public QuestAutoAssignmentService getQuestAutoAssignmentService() {
        return questAutoAssignmentService;
    }

    public QuestAssignmentStore getQuestAssignmentStore() {
        return questAssignmentStore;
    }

    public ComponentType<EntityStore, QuestAssignmentStoreComponent> getQuestAssignmentStoreComponentType() {
        return questAssignmentStoreComponentType;
    }

    public ComponentType<EntityStore, QuestHistoryStoreComponent> getQuestHistoryStoreComponentType() {
        return questHistoryStoreComponentType;
    }
}
