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
import com.martelstudios.hyquests.assets.GatherQuestAsset;
import com.martelstudios.hyquests.assets.GeneralQuestAsset;
import com.martelstudios.hyquests.assets.InteractivelyPickupQuestAsset;
import com.martelstudios.hyquests.assets.QuestAsset;
import com.martelstudios.hyquests.assets.QuestAssetValidator;
import com.martelstudios.hyquests.assets.ReachLocationQuestAsset;
import com.martelstudios.hyquests.commands.QuestCommand;
import com.martelstudios.hyquests.events.GatherItemEventSystem;
import com.martelstudios.hyquests.events.InteractivelyPickupItemEventSystem;
import com.martelstudios.hyquests.events.PlayerEvents;
import com.martelstudios.hyquests.events.QuestAssignedToPlayerEvent;
import com.martelstudios.hyquests.events.QuestEvents;
import com.martelstudios.hyquests.events.QuestHudTickingSystem;
import com.martelstudios.hyquests.events.QuestRegisteredEvent;
import com.martelstudios.hyquests.events.QuestUnregisteredEvent;
import com.martelstudios.hyquests.events.ReachLocationTickingSystem;
import com.martelstudios.hyquests.events.WorldEvents;
import com.martelstudios.hyquests.models.GatherQuest;
import com.martelstudios.hyquests.models.GeneralQuest;
import com.martelstudios.hyquests.models.InteractivelyPickupQuest;
import com.martelstudios.hyquests.models.ReachLocationQuest;
import com.martelstudios.hyquests.rewards.ItemQuestReward;
import com.martelstudios.hyquests.rewards.QuestReward;
import com.martelstudios.hyquests.services.QuestService;
import com.martelstudios.hyquests.stores.QuestDataStore;
import com.martelstudios.hyquests.stores.QuestHistoryStoreComponent;
import com.martelstudios.hyquests.stores.QuestRecord;
import com.martelstudios.hyquests.stores.QuestStore;
import com.martelstudios.hyquests.stores.QuestStoreComponent;
import com.martelstudios.hyquests.stores.WorldQuestStoreResource;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Standalone quest system: quest definitions live as assets, their runtime progression is
 * persisted per instance, and other plugins extend it by registering their own quest types,
 * rewards and asset packs.
 */
public class HyQuestsPlugin extends JavaPlugin {
    public static final Path questProgressionsPath = Paths.get("quests", "progressions");
    public static final Path questStoresPath = Paths.get("quests", "stores");

    private static final String UNIVERSE_QUEST_INDEX_KEY = "universe";
    private static final long SAVE_INTERVAL_MINUTES = 5;

    private static HyQuestsPlugin instance;

    private QuestDataStore questDataStore;
    private DataStore<QuestStore> universeQuestIndexStore;

    public HyQuestsPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
    }

    public static HyQuestsPlugin get() {
        return instance;
    }

    @Override
    protected void setup() {
        super.setup();

        questDataStore = new QuestDataStore(new DiskDataStoreProvider(questProgressionsPath.toString()).create(QuestRecord.CODEC));
        universeQuestIndexStore = new DiskDataStoreProvider(questStoresPath.toString()).create(QuestStore.CODEC);

        ComponentType<EntityStore, QuestStoreComponent> questStoreComponentType = getEntityStoreRegistry().registerComponent(QuestStoreComponent.class, "QuestStore", QuestStoreComponent.CODEC);
        ComponentType<EntityStore, QuestHistoryStoreComponent> questHistoryStoreComponentType = getEntityStoreRegistry().registerComponent(QuestHistoryStoreComponent.class, "QuestHistoryStore", QuestHistoryStoreComponent.CODEC);
        ResourceType<EntityStore, WorldQuestStoreResource> worldQuestStoreResourceType = getEntityStoreRegistry().registerResource(WorldQuestStoreResource.class, "QuestStore", WorldQuestStoreResource.CODEC);
        QuestService.get().init(questDataStore, questStoreComponentType, questHistoryStoreComponentType, worldQuestStoreResourceType);

        registerQuestTypes();
        QuestReward.CODEC.register("Item", ItemQuestReward.class, ItemQuestReward.CODEC);

        getEntityStoreRegistry().registerSystem(new InteractivelyPickupItemEventSystem());
        getEntityStoreRegistry().registerSystem(new GatherItemEventSystem());
        getEntityStoreRegistry().registerSystem(new ReachLocationTickingSystem());
        getEntityStoreRegistry().registerSystem(new QuestHudTickingSystem());

        getCommandRegistry().registerCommand(new QuestCommand());

        getEventRegistry().registerGlobal(LoadAssetEvent.PRIORITY_LOAD_LATE, LoadAssetEvent.class, QuestAssetValidator::handleLoadAsset);
        getEventRegistry().registerGlobal(PlayerConnectEvent.class, PlayerEvents::handlePlayerConnectEvent);
        getEventRegistry().registerGlobal(AddPlayerToWorldEvent.class, WorldEvents::handleAddPlayerToWorldEvent);
        getEventRegistry().registerGlobal(RemovedPlayerFromWorldEvent.class, WorldEvents::handleRemovedPlayerFromWorldEvent);
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
     * {@link QuestService#registerQuestType}.
     */
    private void registerQuestTypes() {
        QuestService.get().registerQuestType("Gather", GatherQuestAsset.class, GatherQuestAsset.CODEC, GatherQuest.class, GatherQuest.CODEC);
        QuestService.get().registerQuestType("InteractivelyPickup", InteractivelyPickupQuestAsset.class, InteractivelyPickupQuestAsset.CODEC, InteractivelyPickupQuest.class, InteractivelyPickupQuest.CODEC);
        QuestService.get().registerQuestType("General", GeneralQuestAsset.class, GeneralQuestAsset.CODEC, GeneralQuest.class, GeneralQuest.CODEC);
        QuestService.get().registerQuestType("ReachLocation", ReachLocationQuestAsset.class, ReachLocationQuestAsset.CODEC, ReachLocationQuest.class, ReachLocationQuest.CODEC);
    }

    @Override
    protected void start() {
        loadUniverseQuests();

        HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(() -> {
            questDataStore.saveAllToDisk();
            saveUniverseQuestIndex();
        }, SAVE_INTERVAL_MINUTES, SAVE_INTERVAL_MINUTES, TimeUnit.MINUTES);
    }

    @Override
    protected void shutdown() {
        if (questDataStore != null) {
            questDataStore.saveAllToDisk();
        }
        saveUniverseQuestIndex();
    }

    /**
     * Loads the universe-scope quest index and pulls every quest it lists into the datastore.
     */
    private void loadUniverseQuests() {
        QuestStore universeIndex;
        try {
            universeIndex = universeQuestIndexStore.load(UNIVERSE_QUEST_INDEX_KEY);
        } catch (IOException e) {
            getLogger().at(Level.WARNING).withCause(e).log("Failed to load universe quest index");
            return;
        }

        if (universeIndex == null) return;

        QuestService.get().universeStore = universeIndex;
        for (UUID questId : universeIndex.getAllIds()) {
            questDataStore.load(questId);
        }
    }

    private void saveUniverseQuestIndex() {
        if (universeQuestIndexStore == null) return;
        universeQuestIndexStore.save(UNIVERSE_QUEST_INDEX_KEY, QuestService.get().universeStore);
    }

    public QuestDataStore getQuestDataStore() {
        return questDataStore;
    }
}
