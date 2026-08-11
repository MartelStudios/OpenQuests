package com.martelstudios.hyquests.services;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.ResourceType;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.hyquests.PlayerAccess;
import com.martelstudios.hyquests.assets.QuestAsset;
import com.martelstudios.hyquests.events.QuestAssignedToPlayerEvent;
import com.martelstudios.hyquests.events.QuestCompletedEvent;
import com.martelstudios.hyquests.events.QuestRegisteredEvent;
import com.martelstudios.hyquests.events.QuestUnregisteredEvent;
import com.martelstudios.hyquests.models.AbstractQuest;
import com.martelstudios.hyquests.models.QuestHistoryRecord;
import com.martelstudios.hyquests.rewards.QuestReward;
import com.martelstudios.hyquests.rewards.QuestRewardContext;
import com.martelstudios.hyquests.stores.QuestDataStore;
import com.martelstudios.hyquests.stores.QuestHistoryStore;
import com.martelstudios.hyquests.stores.QuestHistoryStoreComponent;
import com.martelstudios.hyquests.stores.QuestStore;
import com.martelstudios.hyquests.stores.QuestStoreComponent;
import com.martelstudios.hyquests.stores.WorldQuestStoreResource;
import com.martelstudios.hyquests.visitors.QuestVisitor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Entry point used by event handlers to make quests progress and by anything that wants to
 * hand out quests. Every quest instance lives in the single {@link QuestDataStore} regardless
 * of scope. {@link AbstractQuest#getPlayers()}/{@link AbstractQuest#getWorlds()} are the source
 * of truth for who a quest is assigned to; {@link QuestStoreComponent}/{@link WorldQuestStoreResource}
 * are the reverse index (which quests a player/world has) used to know what to load, and are
 * kept in sync with the quest by every method below.
 * <ul>
 *     <li>Universe scope — shared by everyone. Assigned to every connected player immediately,
 *     and to every future player on connection.</li>
 *     <li>World scope — shared by every player of that world. Assigned to every present player
 *     immediately, and to every future player on entering the world; removed when they leave
 *     unless they also hold the quest directly.</li>
 *     <li>Player scope — belongs to one player, online or not.</li>
 * </ul>
 */
public class QuestService {
    private static QuestService instance;

    /**
     * Quests shared by every player regardless of world. Expected to be rarely used.
     */
    public QuestStore universeStore = new QuestStore();

    private QuestDataStore dataStore;
    private ComponentType<EntityStore, QuestStoreComponent> playerStoreComponentType;
    private ComponentType<EntityStore, QuestHistoryStoreComponent> playerHistoryStoreComponentType;
    private ResourceType<EntityStore, WorldQuestStoreResource> worldStoreResourceType;

    public static QuestService get() {
        if (instance == null) {
            instance = new QuestService();
        }
        return instance;
    }

    private QuestService() {}

    /**
     * Binds the datastore and the player/world scoping handles created by the plugin. Must be called before any other method.
     */
    public void init(@Nonnull QuestDataStore dataStore, @Nonnull ComponentType<EntityStore, QuestStoreComponent> playerStoreComponentType, @Nonnull ComponentType<EntityStore, QuestHistoryStoreComponent> playerHistoryStoreComponentType, @Nonnull ResourceType<EntityStore, WorldQuestStoreResource> worldStoreResourceType) {
        this.dataStore = dataStore;
        this.playerStoreComponentType = playerStoreComponentType;
        this.playerHistoryStoreComponentType = playerHistoryStoreComponentType;
        this.worldStoreResourceType = worldStoreResourceType;
    }

    public ComponentType<EntityStore, QuestStoreComponent> getPlayerStoreComponentType() {
        return playerStoreComponentType;
    }

    public ComponentType<EntityStore, QuestHistoryStoreComponent> getPlayerHistoryStoreComponentType() {
        return playerHistoryStoreComponentType;
    }

    public ResourceType<EntityStore, WorldQuestStoreResource> getWorldStoreResourceType() {
        return worldStoreResourceType;
    }

    /**
     * Registers a concrete quest type's polymorphic serialization tag, for both its asset and
     * its runtime form, under the same id. Mirrors {@code ObjectivePlugin.registerTask}.
     */
    public <Q extends AbstractQuest<Q>, QAsset extends QuestAsset> void registerQuestType(String id, Class<QAsset> questAssetClass, BuilderCodec<QAsset> questAssetCodec, Class<Q> questClass, BuilderCodec<Q> questCodec) {
        QuestAsset.CODEC.register(id, questAssetClass, questAssetCodec);
        AbstractQuest.CODEC.register(id, questClass, questCodec);
    }

    public AbstractQuest<?> loadQuest(UUID questId) {
        return dataStore.load(questId);
    }

    /**
     * Creates and registers a new quest in the store.
     *
     * @param questAsset the quest asset to create the quest from
     */
    public AbstractQuest<?> registerQuest(@Nonnull QuestAsset questAsset) {
        var quest = questAsset.create();
        quest.markDirty();
        registerQuest(quest);
        return quest;
    }

    /**
     * Registers a new quest in the store, so it gets persisted.
     *
     * @param quest the quest to register
     */
    public void registerQuest(@Nonnull AbstractQuest<?> quest) {
        dataStore.add(quest);
        HytaleServer.get()
                    .getEventBus()
                    .dispatchFor(QuestRegisteredEvent.class, quest.getId())
                    .dispatch(new QuestRegisteredEvent(quest));
    }

    /**
     * Unregisters a quest from the store and deletes its persisted file. Clean up all references left in any player/world/universe index.
     *
     * @param questId the id of the quest to unregister
     */
    public AbstractQuest<?> unregisterQuest(@Nonnull UUID questId) {
        AbstractQuest<?> quest = dataStore.get(questId);
        if (quest == null) return null;

        removeQuestFromUniverse(questId);
        for (UUID worldId : Set.copyOf(quest.getWorlds())) {
            removeQuestFromWorld(questId, worldId);
        }

        for (UUID playerId : Set.copyOf(quest.getPlayers())) {
            removeQuestFromPlayer(questId, playerId);
        }

        AbstractQuest<?> removed = dataStore.removeAndDeleteFromDisk(questId);

        HytaleServer.get()
                    .getEventBus()
                    .dispatchFor(QuestUnregisteredEvent.class, quest.getId())
                    .dispatch(new QuestUnregisteredEvent(quest));

        return removed;
    }

    /**
     * Called by {@link AbstractQuest#update} when a quest reaches a terminal state: archives it
     * for every assigned player, then unregisters the instance.
     *
     * @param questId the id of the quest that just completed
     */
    public AbstractQuest<?> completeQuest(@Nonnull UUID questId) {
        AbstractQuest<?> quest = getQuest(questId);
        if (quest == null) return null;

        Set<UUID> players = Set.copyOf(quest.getPlayers());

        unregisterQuest(questId);

        for (UUID playerId : players) {
            archiveForPlayer(quest, playerId);
        }

        HytaleServer.get()
                    .getEventBus()
                    .dispatchFor(QuestCompletedEvent.class, questId)
                    .dispatch(new QuestCompletedEvent(quest));

        return quest;
    }

    /**
     * Runs an action against a live player's components, on their world thread.
     *
     * @return {@code false} if the player is offline, in which case nothing ran: rewards must
     * never be written to stored data, which their entity overwrites on disconnect.
     */
    public boolean withOnlinePlayer(@Nonnull UUID playerId, @Nonnull Consumer<PlayerAccess> action) {
        PlayerRef online = Universe.get().getPlayer(playerId);
        if (online == null) return false;

        var ref = online.getReference();
        if (ref == null) return false;

        var world = ref.getStore().getExternalData().getWorld();
        world.execute(() -> action.accept(PlayerAccess.of(ref)));
        return true;
    }

    /**
     * Writes the quest's completion record into a player's history, online or not. Rewards are
     * only granted to a player who is there to receive them; an offline player keeps a claimable
     * record, collected by {@link #claimAutoRewards} on their next connection.
     */
    private void archiveForPlayer(@Nonnull AbstractQuest<?> quest, @Nonnull UUID playerId) {
        var record = new QuestHistoryRecord(quest);
        var asset = quest.getAsset();
        boolean autoClaim = asset != null && asset.isAutoClaim();

        boolean online = withOnlinePlayer(playerId, access -> {
            if (autoClaim) {
                grantRewards(asset.getRewards(quest.getState()), new QuestRewardContext(playerId, access));
                record.setClaimed(true);
            }

            access.ensureAndGetComponent(playerHistoryStoreComponentType).questHistoryStore.register(record);
        });

        if (online) return;

        Universe.get()
                .getPlayerStorage()
                .update(playerId, holder -> holder.ensureAndGetComponent(playerHistoryStoreComponentType).questHistoryStore.register(record));
    }

    /**
     * Grants what a player earned while they were away. Called once they are connected, since
     * completions that happened offline could not hand their rewards over at the time.
     */
    public void claimAutoRewards(@Nonnull UUID playerId, @Nonnull PlayerAccess access) {
        var history = access.ensureAndGetComponent(playerHistoryStoreComponentType).questHistoryStore;

        for (QuestHistoryRecord record : history.getAll()) {
            if (!record.isClaimable()) continue;

            QuestAsset asset = QuestAsset.getAsset(record.getQuestAssetId());
            if (asset == null || !asset.isAutoClaim()) continue;

            grantRewards(asset.getRewards(record.getState()), new QuestRewardContext(playerId, access));
            record.setClaimed(true);
        }
    }

    /**
     * Claims a completion's rewards, if it is still claimable. Marks the record as claimed, so
     * rewards can never be granted twice.
     */
    public void claimRewards(@Nonnull UUID questId, @Nonnull UUID playerId) {
        withOnlinePlayer(playerId, access -> {
            var history = access.ensureAndGetComponent(playerHistoryStoreComponentType).questHistoryStore;

            QuestHistoryRecord record = history.get(questId);
            if (record == null || !record.isClaimable()) return;

            QuestAsset asset = QuestAsset.getAsset(record.getQuestAssetId());
            if (asset == null) return;

            grantRewards(asset.getRewards(record.getState()), new QuestRewardContext(playerId, access));
            record.setClaimed(true);
        });
    }

    private void grantRewards(@Nonnull QuestReward[] rewards, @Nonnull QuestRewardContext context) {
        for (QuestReward reward : rewards) {
            reward.grant(context);
        }
    }

    /**
     * @return the completion history of a player, or {@code null} if they have none yet.
     */
    @Nullable
    public QuestHistoryStore getHistory(@Nonnull Holder<EntityStore> holder) {
        var component = holder.getComponent(playerHistoryStoreComponentType);
        return component == null ? null : component.questHistoryStore;
    }

    /**
     * Adds a quest to a player's index (whether they're currently online or not) and adds the
     * player to the quest's own player list.
     */
    public void addQuestToPlayer(@Nonnull UUID questId, @Nonnull UUID playerId) {
        AbstractQuest<?> quest = dataStore.get(questId);
        if (quest == null) return;

        quest.addToPlayer(playerId);
    }

    /**
     * Removes a quest from a player's index and removes the player from the quest's own player list.
     */
    public void removeQuestFromPlayer(@Nonnull UUID questId, @Nonnull UUID playerId) {
        AbstractQuest<?> quest = dataStore.get(questId);
        if (quest == null) return;

        quest.removeFromPlayer(playerId);
    }

    /**
     * Adds a quest to a world's index, adds the world to the quest's own world list, and adds
     * the quest to every player currently present in that world.
     */
    public void addQuestToWorld(@Nonnull UUID questId, @Nonnull UUID worldId) {
        var world = Universe.get().getWorld(worldId);
        if (world == null) return;

        AbstractQuest<?> quest = dataStore.get(questId);
        if (quest == null) return;

        quest.getWorlds().add(worldId);

        world.execute(() -> {
            var store = world.getEntityStore().getStore();
            store.getResource(worldStoreResourceType).questStore.register(questId);

            for (PlayerRef player : world.getPlayerRefs()) {
                var ref = player.getReference();
                if (ref != null) addQuestToPlayerStore(questId, ref);
            }
        });
    }

    /**
     * Removes a quest from a world's index, removes the world from the quest's own world list,
     * and removes the quest from every player present in that world, unless they also hold the
     * quest directly (in the quest's own player list).
     */
    public void removeQuestFromWorld(@Nonnull UUID questId, @Nonnull UUID worldId) {
        var world = Universe.get().getWorld(worldId);
        if (world == null) return;

        AbstractQuest<?> quest = dataStore.get(questId);
        if (quest == null) return;

        quest.getWorlds().remove(worldId);

        world.execute(() -> {
            var store = world.getEntityStore().getStore();
            store.getResource(worldStoreResourceType).questStore.unregister(questId);

            for (PlayerRef player : world.getPlayerRefs()) {
                if (quest.getPlayers().contains(player.getUuid())) continue;

                var ref = player.getReference();
                if (ref != null) removeQuestFromPlayerStore(questId, ref);
            }
        });
    }

    /**
     * Adds a quest to the universe's index and to every currently connected player.
     */
    public void addQuestToUniverse(@Nonnull UUID questId) {
        AbstractQuest<?> quest = dataStore.get(questId);
        if (quest == null) return;

        universeStore.register(questId);

        for (PlayerRef player : Universe.get().getPlayers()) {
            var ref = player.getReference();
            if (ref == null) continue;
            var world = ref.getStore().getExternalData().getWorld();
            world.execute(() -> addQuestToPlayerStore(questId, ref));
        }
    }

    /**
     * Removes a quest from the universe's index and from every connected player, unless they
     * hold it directly (in the quest's own player list) or through a world they're currently in
     * (in the quest's own world list).
     */
    public void removeQuestFromUniverse(@Nonnull UUID questId) {
        AbstractQuest<?> quest = dataStore.get(questId);
        if (quest == null) return;

        universeStore.unregister(questId);

        for (PlayerRef player : Universe.get().getPlayers()) {
            // If the quest holds a direct player reference, skip the player
            if (quest.getPlayers().contains(player.getUuid())) continue;

            // If the quest holds a world reference of the player's current world, skip the player
            UUID currentWorldUuid = player.getWorldUuid();
            if (currentWorldUuid != null && quest.getWorlds().contains(currentWorldUuid)) continue;

            // Otherwise, remove the quest from the player's store
            var ref = player.getReference();
            if (ref == null) continue;
            var world = ref.getStore().getExternalData().getWorld();
            world.execute(() -> removeQuestFromPlayerStore(questId, ref));
        }
    }

    public AbstractQuest<?> getQuest(UUID questId) {
        return dataStore.get(questId);
    }

    @SuppressWarnings("unchecked")
    public <Q extends AbstractQuest<Q>> void progress(QuestVisitor<Q> visitor) {
        for (UUID id : dataStore.getForType(visitor.getQuestType())) {
            AbstractQuest<?> quest = dataStore.get(id);
            if (quest == null) continue;
            ((Q) quest).update(visitor);
        }
    }

    /**
     * Must be called on the world thread the given ref belongs to (e.g. inside {@link World#execute}).
     */
    public void addQuestToPlayerStore(@Nonnull UUID questId, @Nonnull Ref<EntityStore> ref) {
        var playerStoreComponent = ref.getStore().ensureAndGetComponent(ref, playerStoreComponentType);
        var playerId = ref.getStore().getComponent(ref, UUIDComponent.getComponentType()).getUuid();
        addQuestToPlayerStore(playerStoreComponent, questId, playerId);
    }

    public void addQuestToPlayerStore(@Nonnull UUID questId, @Nonnull Holder<EntityStore> holder) {
        var playerStoreComponent = holder.ensureAndGetComponent(playerStoreComponentType);
        var playerId = holder.getComponent(UUIDComponent.getComponentType()).getUuid();
        addQuestToPlayerStore(playerStoreComponent, questId, playerId);
    }

    /**
     * Must be called on the world thread the given ref belongs to (e.g. inside {@link World#execute}).
     */
    public void addQuestsToPlayerStore(@Nonnull Collection<UUID> questIds, @Nonnull Ref<EntityStore> ref) {
        var playerStoreComponent = ref.getStore().ensureAndGetComponent(ref, playerStoreComponentType);
        var playerId = ref.getStore().getComponent(ref, UUIDComponent.getComponentType()).getUuid();
        questIds.forEach(questId -> addQuestToPlayerStore(playerStoreComponent, questId, playerId));
    }

    public void addQuestsToPlayerStore(@Nonnull Collection<UUID> questIds, @Nonnull Holder<EntityStore> holder) {
        var playerStoreComponent = holder.ensureAndGetComponent(playerStoreComponentType);
        var playerId = holder.getComponent(UUIDComponent.getComponentType()).getUuid();
        questIds.forEach(questId -> addQuestToPlayerStore(playerStoreComponent, questId, playerId));
    }

    public void addQuestToPlayerStore(QuestStoreComponent playerStore, UUID questId, UUID playerId) {
        AbstractQuest<?> quest = dataStore.get(questId);

        if (quest == null) return;

        playerStore.questStore.register(questId);

        HytaleServer.get()
                    .getEventBus()
                    .dispatchFor(QuestAssignedToPlayerEvent.class, playerId)
                    .dispatch(new QuestAssignedToPlayerEvent(quest, playerId));
    }

    /**
     * Must be called on the world thread the given ref belongs to (e.g. inside {@link World#execute}).
     */
    public void removeQuestFromPlayerStore(@Nonnull UUID questId, @Nonnull Ref<EntityStore> ref) {
        ref.getStore().ensureAndGetComponent(ref, playerStoreComponentType).questStore.unregister(questId);
    }

    /**
     * Must be called on the world thread the given ref belongs to (e.g. inside {@link World#execute}).
     */
    public void removeQuestFromPlayerStore(@Nonnull UUID questId, @Nonnull Holder<EntityStore> holder) {
        holder.ensureAndGetComponent(playerStoreComponentType).questStore.unregister(questId);
    }

    /**
     * Must be called on the world thread the given ref belongs to (e.g. inside {@link World#execute}).
     */
    public void removeQuestFromPlayerStore(@Nonnull Collection<UUID> questIds, @Nonnull Ref<EntityStore> ref) {
        var playerStore = ref.getStore().ensureAndGetComponent(ref, playerStoreComponentType);
        questIds.forEach(playerStore.questStore::unregister);
    }

    /**
     * Must be called on the world thread the given ref belongs to (e.g. inside {@link World#execute}).
     */
    public void removeQuestFromPlayerStore(@Nonnull Collection<UUID> questIds, @Nonnull Holder<EntityStore> holder) {
        var playerStore = holder.ensureAndGetComponent(playerStoreComponentType);
        questIds.forEach(playerStore.questStore::unregister);
    }
}
