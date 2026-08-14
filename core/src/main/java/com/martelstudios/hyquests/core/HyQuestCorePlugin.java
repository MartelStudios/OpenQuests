package com.martelstudios.hyquests.core;

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
import com.martelstudios.hyquests.core.assets.QuestAsset;
import com.martelstudios.hyquests.core.commands.QuestCommand;
import com.martelstudios.hyquests.core.events.QuestCompletedEvent;
import com.martelstudios.hyquests.core.services.PlayerQuestService;
import com.martelstudios.hyquests.core.services.QuestHistoryService;
import com.martelstudios.hyquests.core.services.QuestProgressionService;
import com.martelstudios.hyquests.core.services.UniverseQuestService;
import com.martelstudios.hyquests.core.services.WorldQuestService;
import com.martelstudios.hyquests.core.stores.QuestHistoryStoreComponent;
import com.martelstudios.hyquests.core.stores.QuestProgressionRecord;
import com.martelstudios.hyquests.core.stores.QuestProgressionStore;
import com.martelstudios.hyquests.core.stores.QuestStoreComponent;
import com.martelstudios.hyquests.core.stores.QuestsRecord;
import com.martelstudios.hyquests.core.stores.QuestsStore;
import com.martelstudios.hyquests.core.stores.WorldQuestStoreResource;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

/**
 * The quest system itself: definitions as assets, per-instance runtime progression, scopes and
 * rewards. Ships no quest type of its own — those are registered on top, by
 * {@code HyQuestExtensionPlugin} or by any other plugin.
 */
public class HyQuestCorePlugin extends JavaPlugin {
    public static final Path questProgressionsPath = Paths.get("quests", "progressions");
    public static final Path questSetStorePath = Paths.get("quests", "stores");

    private static final long SAVE_INTERVAL_MINUTES = 5;

    private static HyQuestCorePlugin instance;

    private QuestProgressionStore questProgressionStore;
    private QuestsStore questsStore;
    private ComponentType<EntityStore, QuestStoreComponent> questStoreComponentType;
    private ResourceType<EntityStore, WorldQuestStoreResource> worldStoreResourceType;
    private ComponentType<EntityStore, QuestHistoryStoreComponent> questHistoryStoreComponentType;

    private QuestProgressionService questProgressionService;
    private QuestHistoryService questHistoryService;
    private UniverseQuestService universeQuestService;
    private WorldQuestService worldQuestService;
    private PlayerQuestService playerQuestService;

    public HyQuestCorePlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
    }

    public static HyQuestCorePlugin get() {
        return instance;
    }

    @Override
    protected void setup() {
        super.setup();
        questProgressionStore = new QuestProgressionStore(new DiskDataStoreProvider(questProgressionsPath.toString()).create(QuestProgressionRecord.CODEC));
        questsStore = new QuestsStore(new DiskDataStoreProvider(questSetStorePath.toString()).create(QuestsRecord.CODEC));

        questProgressionService = new QuestProgressionService(questProgressionStore);
        questHistoryService = new QuestHistoryService();
        universeQuestService = new UniverseQuestService(questsStore);
        worldQuestService = new WorldQuestService();
        playerQuestService = new PlayerQuestService();

        questStoreComponentType = getEntityStoreRegistry().registerComponent(QuestStoreComponent.class, "QuestStore", QuestStoreComponent.CODEC);
        worldStoreResourceType = getEntityStoreRegistry().registerResource(WorldQuestStoreResource.class, "QuestStore", WorldQuestStoreResource.CODEC);
        questHistoryStoreComponentType = getEntityStoreRegistry().registerComponent(QuestHistoryStoreComponent.class, "QuestHistoryStore", QuestHistoryStoreComponent.CODEC);

        getCommandRegistry().registerCommand(new QuestCommand());

        // All wiring lives here, so the plugin registry unregisters it on disable
        getEventRegistry().registerGlobal(PlayerConnectEvent.class, playerQuestService::handlePlayerConnectEvent);
        getEventRegistry().registerGlobal(PlayerConnectEvent.class, universeQuestService::handlePlayerConnectEvent);
        getEventRegistry().registerGlobal(AddPlayerToWorldEvent.class, worldQuestService::handleAddPlayerToWorldEvent);
        getEventRegistry().registerGlobal(AddPlayerToWorldEvent.class, questHistoryService::handleAddPlayerToWorldEvent);
        getEventRegistry().registerGlobal(RemovedPlayerFromWorldEvent.class, worldQuestService::handleRemovedPlayerFromWorldEvent);
        getEventRegistry().registerGlobal(QuestCompletedEvent.class, questHistoryService::handleQuestCompletedEvent);

        getAssetRegistry().register(HytaleAssetStore.builder(QuestAsset.class, new DefaultAssetMap<>())
                                                    .setPath("HyQuests/Quests/")
                                                    .setCodec(QuestAsset.CODEC)
                                                    .setKeyFunction(QuestAsset::getId)
                                                    .build());
    }

    @Override
    protected void start() {
        universeQuestService.loadQuests();

        HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(() -> {
            questProgressionStore.saveAllToDisk();
            universeQuestService.saveUniverseQuestIndex();
        }, SAVE_INTERVAL_MINUTES, SAVE_INTERVAL_MINUTES, TimeUnit.MINUTES);
    }

    @Override
    protected void shutdown() {
        questProgressionStore.saveAllToDisk();
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

    public ComponentType<EntityStore, QuestHistoryStoreComponent> getQuestHistoryStoreComponentType() {
        return questHistoryStoreComponentType;
    }
}
