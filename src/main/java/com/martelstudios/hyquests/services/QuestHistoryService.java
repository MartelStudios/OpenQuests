package com.martelstudios.hyquests.services;

import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.hyquests.HyQuestsPlugin;
import com.martelstudios.hyquests.assets.QuestAsset;
import com.martelstudios.hyquests.events.QuestCompletedEvent;
import com.martelstudios.hyquests.models.AbstractQuest;
import com.martelstudios.hyquests.models.QuestHistoryRecord;
import com.martelstudios.hyquests.rewards.QuestReward;
import com.martelstudios.hyquests.stores.QuestHistoryStore;
import com.martelstudios.hyquests.stores.QuestHistoryStoreComponent;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Archives completed quests per player and hands over their rewards. Rewards only ever reach a
 * live player: an offline one keeps a claimable record, collected on their next world entry.
 */
public class QuestHistoryService {

    public static QuestHistoryService get() {
        return HyQuestsPlugin.get().getQuestHistoryService();
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
     * Hands over what completions that happened while the player was away could not grant. Done
     * on world entry rather than on connection so they are there to be shown the reward.
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
     * Grants what a player earned while they were away. Called once they are connected, since
     * completions that happened offline could not hand their rewards over at the time.
     */
    public void claimAutoRewards(@Nonnull Ref<EntityStore> playerRef) {
        var history = getHistory(playerRef);
        for (QuestHistoryRecord record : history.getAll()) {
            QuestAsset asset = record.getAsset();

            // The asset is gone, so nothing can ever be granted for it: settle the record
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
     * Marks the record either way it ends, so rewards can never be granted twice.
     */
    private void grantRewards(@Nonnull QuestHistoryRecord record, @Nonnull Ref<EntityStore> playerRef) {
        if (record.isClaimed()) return;

        // Nothing to hand over for this outcome, but the record is settled all the same
        if (!record.isClaimable()) {
            record.setClaimed(true);
            return;
        }

        var asset = record.getAsset();
        for (QuestReward reward : asset.getRewards(record.getState())) {
            reward.grant(asset, playerRef);
        }

        record.setClaimed(true);
    }
}
