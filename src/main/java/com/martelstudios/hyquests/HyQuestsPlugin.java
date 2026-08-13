package com.martelstudios.hyquests;

import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.ResourceType;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import com.hypixel.hytale.server.core.asset.LoadAssetEvent;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.RemovedPlayerFromWorldEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.datastore.DataStore;
import com.hypixel.hytale.server.core.universe.datastore.DiskDataStoreProvider;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.hyquests.assets.*;
import com.martelstudios.hyquests.commands.QuestCommand;
import com.martelstudios.hyquests.events.*;
import com.martelstudios.hyquests.models.GatherQuest;
import com.martelstudios.hyquests.models.GeneralQuest;
import com.martelstudios.hyquests.models.InteractivelyPickupQuest;
import com.martelstudios.hyquests.models.ReachLocationQuest;
import com.martelstudios.hyquests.rewards.ItemQuestReward;
import com.martelstudios.hyquests.rewards.QuestReward;
import com.martelstudios.hyquests.services.PlayerQuestService;
import com.martelstudios.hyquests.services.QuestProgressionService;
import com.martelstudios.hyquests.services.UniverseQuestService;
import com.martelstudios.hyquests.services.WorldQuestService;
import com.martelstudios.hyquests.stores.*;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

/**
 * Standalone quest system: quest definitions live as assets, their runtime progression is
 * persisted per instance, and other plugins extend it by registering their own quest types,
 * rewards and asset packs.
 */
public class HyQuestsPlugin extends JavaPlugin {
    public static final Path questProgressionsPath = Paths.get("quests", "progressions");
    public static final Path questSetStorePath = Paths.get("quests", "stores");

    private static final long SAVE_INTERVAL_MINUTES = 5;

    private static HyQuestsPlugin instance;

    private QuestProgressionStore questProgressionStore;
    private QuestsStore questsStore;
    private ComponentType<EntityStore, QuestStoreComponent> questStoreComponentType;
    private ResourceType<EntityStore, WorldQuestStoreResource> worldStoreResourceType;
    private DataStore<QuestsRecord> questSetStore;

    public HyQuestsPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
    }

    public static HyQuestsPlugin get() {
        return instance;
    }

    public UniverseQuestService universeQuestService;
    public WorldQuestService worldQuestService;
    public PlayerQuestService playerQuestService;

    public QuestProgressionService questProgressionService;

    @Override
    protected void setup() {
        super.setup();
        questProgressionStore = new QuestProgressionStore(new DiskDataStoreProvider(questProgressionsPath.toString()).create(QuestProgressionRecord.CODEC));
        questsStore = new QuestsStore(new DiskDataStoreProvider(questSetStorePath.toString()).create(QuestsRecord.CODEC));

        universeQuestService = new UniverseQuestService(questsStore);
        worldQuestService = new WorldQuestService();
        playerQuestService = new PlayerQuestService();

        questStoreComponentType = getEntityStoreRegistry().registerComponent(QuestStoreComponent.class, "QuestStore", QuestStoreComponent.CODEC);
        worldStoreResourceType = getEntityStoreRegistry().registerResource(WorldQuestStoreResource.class, "QuestStore", WorldQuestStoreResource.CODEC);
        ComponentType<EntityStore, QuestHistoryStoreComponent> questHistoryStoreComponentType = getEntityStoreRegistry().registerComponent(QuestHistoryStoreComponent.class, "QuestHistoryStore", QuestHistoryStoreComponent.CODEC);
        questProgressionService = new QuestProgressionService(questProgressionStore, questHistoryStoreComponentType);

        registerQuestTypes();
        QuestReward.CODEC.register("Item", ItemQuestReward.class, ItemQuestReward.CODEC);

        getEntityStoreRegistry().registerSystem(new InteractivelyPickupItemEventSystem());
        getEntityStoreRegistry().registerSystem(new GatherItemEventSystem());
        getEntityStoreRegistry().registerSystem(new ReachLocationTickingSystem());
        getEntityStoreRegistry().registerSystem(new QuestHudTickingSystem());

        getCommandRegistry().registerCommand(new QuestCommand());

        // All wiring lives here, so the plugin registry unregisters it on disable
        getEventRegistry().registerGlobal(LoadAssetEvent.PRIORITY_LOAD_LATE, LoadAssetEvent.class, QuestAssetValidator::handleLoadAsset);
        getEventRegistry().registerGlobal(PlayerConnectEvent.class, PlayerEvents::handlePlayerConnectEvent);
        getEventRegistry().registerGlobal(PlayerConnectEvent.class, universeQuestService::handlePlayerConnectEvent);
        getEventRegistry().registerGlobal(AddPlayerToWorldEvent.class, worldQuestService::handleAddPlayerToWorldEvent);
        getEventRegistry().registerGlobal(AddPlayerToWorldEvent.class, WorldEvents::handleAddPlayerToWorldEvent);
        getEventRegistry().registerGlobal(RemovedPlayerFromWorldEvent.class, worldQuestService::handleRemovedPlayerFromWorldEvent);
        getEventRegistry().registerGlobal(QuestAssignedToPlayerEvent.class, QuestEvents::handleQuestAssignedToPlayer);
        getEventRegistry().registerGlobal(QuestRegisteredEvent.class, QuestEvents::handleQuestRegistered);
        getEventRegistry().registerGlobal(QuestUnregisteredEvent.class, QuestEvents::handleQuestUnregistered);

        getAssetRegistry().register(HytaleAssetStore.builder(QuestAsset.class, new DefaultAssetMap<>())
                                                    .setPath("HyQuests/Quests/")
                                                    .setCodec(QuestAsset.CODEC)
                                                    .setKeyFunction(QuestAsset::getId)
                                                    .build());
    }

    /**
     * The quest types shipped by default. Other plugins add theirs the same way, through
     * {@link QuestProgressionService#registerQuestType}.
     */
    private void registerQuestTypes() {
        QuestProgressionService.get()
                               .registerQuestType("Gather", GatherQuestAsset.class, GatherQuestAsset.CODEC, GatherQuest.class, GatherQuest.CODEC);
        QuestProgressionService.get()
                               .registerQuestType("InteractivelyPickup", InteractivelyPickupQuestAsset.class, InteractivelyPickupQuestAsset.CODEC, InteractivelyPickupQuest.class, InteractivelyPickupQuest.CODEC);
        QuestProgressionService.get()
                               .registerQuestType("General", GeneralQuestAsset.class, GeneralQuestAsset.CODEC, GeneralQuest.class, GeneralQuest.CODEC);
        QuestProgressionService.get()
                               .registerQuestType("ReachLocation", ReachLocationQuestAsset.class, ReachLocationQuestAsset.CODEC, ReachLocationQuest.class, ReachLocationQuest.CODEC);
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

    public QuestProgressionStore getQuestDataStore() {
        return questProgressionStore;
    }

    public ComponentType<EntityStore, QuestStoreComponent> getQuestStoreComponentType() {
        return questStoreComponentType;
    }

    public ResourceType<EntityStore, WorldQuestStoreResource> getWorldStoreResourceType() {
        return worldStoreResourceType;
    }
}
