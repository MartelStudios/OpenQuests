package com.martelstudios.openquests.core;

import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.ResourceType;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.datastore.DiskDataStoreProvider;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.openquests.core.models.QuestAsset;
import com.martelstudios.openquests.core.commands.QuestCommand;
import com.martelstudios.openquests.core.history.services.QuestHistoryService;
import com.martelstudios.openquests.core.history.stores.QuestHistoryStoreComponent;
import com.martelstudios.openquests.core.scopes.player.PlayerQuestService;
import com.martelstudios.openquests.core.scopes.universe.QuestsStore;
import com.martelstudios.openquests.core.scopes.universe.UniverseQuestService;
import com.martelstudios.openquests.core.scopes.world.WorldQuestService;
import com.martelstudios.openquests.core.scopes.world.WorldQuestStoreResource;
import com.martelstudios.openquests.core.services.QuestAutoStartService;
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

    private static final long SAVE_INTERVAL_MINUTES = 5;

    private static OpenQuestCorePlugin instance;

    private QuestProgressionStore questProgressionStore;
    private QuestsStore questsStore;
    private ComponentType<EntityStore, QuestStoreComponent> questStoreComponentType;
    private ResourceType<EntityStore, WorldQuestStoreResource> worldStoreResourceType;
    private ComponentType<EntityStore, QuestHistoryStoreComponent> questHistoryStoreComponentType;

    private QuestProgressionService questProgressionService;
    private QuestAutoStartService questAutoStartService;
    private QuestHistoryService questHistoryService;
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
        universeQuestService = new UniverseQuestService(this, questsStore);
        worldQuestService = new WorldQuestService(this);
        playerQuestService = new PlayerQuestService(this);
        questAutoStartService = new QuestAutoStartService(this);

        questStoreComponentType = getEntityStoreRegistry().registerComponent(QuestStoreComponent.class, "QuestStore", QuestStoreComponent.CODEC);
        worldStoreResourceType = getEntityStoreRegistry().registerResource(WorldQuestStoreResource.class, "QuestStore", WorldQuestStoreResource.CODEC);
        questHistoryStoreComponentType = getEntityStoreRegistry().registerComponent(QuestHistoryStoreComponent.class, "QuestHistoryStore", QuestHistoryStoreComponent.CODEC);

        getCommandRegistry().registerCommand(new QuestCommand());


        getAssetRegistry().register(HytaleAssetStore.builder(QuestAsset.class, new DefaultAssetMap<>())
                                                    .setPath("OpenQuests/Quests/")
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

    public QuestAutoStartService getQuestAutoStartService() {
        return questAutoStartService;
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
