package com.martelstudios.hyquests.core.services;

import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.hyquests.core.HyQuestCorePlugin;
import com.martelstudios.hyquests.core.assets.QuestAsset;
import com.martelstudios.hyquests.core.events.QuestCompletedEvent;
import com.martelstudios.hyquests.core.models.AbstractQuest;
import com.martelstudios.hyquests.core.models.QuestHistoryRecord;
import com.martelstudios.hyquests.core.rewards.QuestReward;
import com.martelstudios.hyquests.core.stores.QuestHistoryStore;
import com.martelstudios.hyquests.core.stores.QuestHistoryStoreComponent;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Archives completed quests per player and hands over their rewards. Rewards only ever reach a
 * live player: an offline one keeps a claimable record, collected on their next world entry.
 */
public class QuestHistoryService {

    public static QuestHistoryService get() {
        return HyQuestCorePlugin.get().getQuestHistoryService();
    }

    /**
     * @return the completion history of a live player, created if they have none yet.
     */
    @Nonnull
    public QuestHistoryStore getHistory(@Nonnull Ref<EntityStore> playerRef) {
        return playerRef.getStore()
                        .ensureAndGetComponent(playerRef, QuestHistoryStoreComponent.getComponentType()).history;
    }

    /**
     * @return the completion history of a player loaded from storage, created if they have none yet.
     */
    @Nonnull
    public QuestHistoryStore getHistory(@Nonnull Holder<EntityStore> playerHolder) {
        return playerHolder.ensureAndGetComponent(QuestHistoryStoreComponent.getComponentType()).history;
    }

    public void handleQuestCompletedEvent(QuestCompletedEvent questCompletedEvent) {
        var quest = questCompletedEvent.getQuest();
        for (UUID playerId : quest.getPlayers()) {
            archiveQuestForPlayer(quest, playerId);
        }
    }

    /**
     * On world entry rather than on connection, so the player is there to be shown the reward.
     */
    public void handleAddPlayerToWorldEvent(@Nonnull AddPlayerToWorldEvent addPlayerToWorldEvent) {
        var playerRef = addPlayerToWorldEvent.getHolder().getComponent(PlayerRef.getComponentType());
        if (playerRef == null || playerRef.getReference() == null) return;

        claimAutoRewards(playerRef.getReference());
    }

    private void archiveQuestForPlayer(@Nonnull AbstractQuest<?> quest, @Nonnull UUID playerId) {
        PlayerRef playerRef = Universe.get().getPlayer(playerId);
        if (playerRef != null && playerRef.getReference() != null) {
            archiveQuestForOnlinePlayer(quest, playerRef.getReference());
        } else {
            archiveQuestForOfflinePlayer(quest, playerId);
        }
    }

    private void archiveQuestForOnlinePlayer(@Nonnull AbstractQuest<?> quest, @Nonnull Ref<EntityStore> playerRef) {
        var world = playerRef.getStore().getExternalData().getWorld();

        var asset = quest.getAsset();
        boolean autoClaim = asset != null && asset.isAutoClaim();

        world.execute(() -> {
            var record = new QuestHistoryRecord(quest);
            getHistory(playerRef).register(record);

            if (autoClaim) grantRewards(record, playerRef);
        });
    }

    private void archiveQuestForOfflinePlayer(@Nonnull AbstractQuest<?> quest, @Nonnull UUID playerId) {
        var record = new QuestHistoryRecord(quest);
        Universe.get().getPlayerStorage().update(playerId, holder -> getHistory(holder).register(record));
    }

    /**
     * Claim all completed quests that are auto-claimable
     */
    public void claimAutoRewards(@Nonnull Ref<EntityStore> playerRef) {
        var history = getHistory(playerRef);
        for (QuestHistoryRecord record : history.getAll()) {
            QuestAsset asset = record.getAsset();

            if (asset == null) {
                history.unregister(record);
                continue;
            }

            if (!asset.isAutoClaim()) continue;

            grantRewards(record, playerRef);
        }
    }

    /**
     * Claims one completion on the player's request, for quests that are not {@code AutoClaim}.
     *
     * @param questId the id the quest had while it was live
     * @return {@code false} if that completion is unknown or has nothing left to give
     */
    public boolean claimRewards(@Nonnull UUID questId, @Nonnull Ref<EntityStore> playerRef) {
        QuestHistoryRecord record = getHistory(playerRef).get(questId);
        if (record == null || !record.isClaimable()) return false;

        grantRewards(record, playerRef);
        return true;
    }

    /**
     * Updates the record after each success, so a reward sees what the previous ones granted.
     */
    private void grantRewards(@Nonnull QuestHistoryRecord record, @Nonnull Ref<EntityStore> playerRef) {
        if (record.isClaimed()) return;

        QuestReward[] pending = record.getPendingRewards();
        List<QuestReward> remaining = Arrays.asList(pending);

        for (QuestReward reward : pending) {
            if (!reward.grant(record, playerRef)) continue;

            remaining.remove(reward);
            record.setPendingRewards(remaining.toArray(new QuestReward[0]));
        }
    }
}
