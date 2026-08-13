package com.martelstudios.hyquests.services;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.hyquests.HyQuestsPlugin;
import com.martelstudios.hyquests.PlayerAccess;
import com.martelstudios.hyquests.assets.QuestAsset;
import com.martelstudios.hyquests.events.QuestCompletedEvent;
import com.martelstudios.hyquests.events.QuestRegisteredEvent;
import com.martelstudios.hyquests.events.QuestUnregisteredEvent;
import com.martelstudios.hyquests.models.AbstractQuest;
import com.martelstudios.hyquests.models.QuestHistoryRecord;
import com.martelstudios.hyquests.rewards.QuestReward;
import com.martelstudios.hyquests.rewards.QuestRewardContext;
import com.martelstudios.hyquests.stores.*;
import com.martelstudios.hyquests.visitors.QuestVisitor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Entry point used by event handlers to make quests progress and by anything that wants to
 * hand out quests. Every quest instance lives in the single {@link QuestProgressionStore} regardless
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
public class QuestProgressionService {
    private static QuestProgressionService instance;

    private QuestProgressionStore dataStore;
    private ComponentType<EntityStore, QuestHistoryStoreComponent> playerHistoryStoreComponentType;

    public QuestProgressionService(QuestProgressionStore questProgressionStore, @Nonnull ComponentType<EntityStore, QuestHistoryStoreComponent> playerHistoryStoreComponentType) {
        this.dataStore = questProgressionStore;
        this.playerHistoryStoreComponentType = playerHistoryStoreComponentType;
    }

    public static QuestProgressionService get() {
        return HyQuestsPlugin.get().questProgressionService;
    }

    public ComponentType<EntityStore, QuestHistoryStoreComponent> getPlayerHistoryStoreComponentType() {
        return playerHistoryStoreComponentType;
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

        UniverseQuestService.get().removeQuestFromUniverse(questId);
        quest.removeAllWorlds();
        quest.removeAllPlayers();

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
}
