package com.martelstudios.openquests.core.history.services;

import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.martelstudios.openquests.core.OpenQuestCorePlugin;
import com.martelstudios.openquests.core.models.QuestAsset;
import com.martelstudios.openquests.core.events.QuestCompletedEvent;
import com.martelstudios.openquests.core.history.models.QuestHistoryRecord;
import com.martelstudios.openquests.core.history.stores.QuestHistoryStore;
import com.martelstudios.openquests.core.history.stores.QuestHistoryStoreComponent;
import com.martelstudios.openquests.core.rewards.QuestReward;
import com.martelstudios.openquests.core.utils.EntityComponents;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Archives completed quests per player and hands over their rewards. Rewards only ever reach a
 * live player: an offline one keeps a claimable record, collected on their next world entry.
 */
public class QuestHistoryService {

    public QuestHistoryService(JavaPlugin plugin) {
        plugin.getEventRegistry().registerGlobal(AddPlayerToWorldEvent.class, this::handleAddPlayerToWorldEvent);
        plugin.getEventRegistry().registerGlobal(QuestCompletedEvent.class, this::handleQuestCompletedEvent);
    }

    public static QuestHistoryService get() {
        return OpenQuestCorePlugin.get().getQuestHistoryService();
    }

    /**
     * @return the completion history of a player, created if they have none yet.
     */
    @Nonnull
    public QuestHistoryStore getHistory(@Nonnull EntityComponents playerComponents) {
        return playerComponents.ensureAndGetComponent(QuestHistoryStoreComponent.getComponentType()).history;
    }

    private void handleQuestCompletedEvent(QuestCompletedEvent questCompletedEvent) {
        var quest = questCompletedEvent.getQuest();
        var asset = quest.getAsset();
        boolean autoClaim = asset != null && asset.isAutoClaim();
        boolean persistHistory = quest.isPersistHistory();

        for (UUID playerId : quest.getPlayers()) {
            EntityComponents.update(playerId, components -> {
                var record = new QuestHistoryRecord(quest);
                if (persistHistory) getHistory(components).register(record);

                if (autoClaim) grantRewards(record, components);
            });
        }
    }

    /**
     * On world entry rather than on connection, so the player is there to be shown the reward.
     */
    private void handleAddPlayerToWorldEvent(@Nonnull AddPlayerToWorldEvent addPlayerToWorldEvent) {
        var playerRef = addPlayerToWorldEvent.getHolder().getComponent(PlayerRef.getComponentType());
        if (playerRef == null || playerRef.getReference() == null) return;

        claimAutoRewards(EntityComponents.of(playerRef.getReference()));
    }

    /**
     * Claim all completed quests that are auto-claimable
     */
    public void claimAutoRewards(@Nonnull EntityComponents playerComponents) {
        var history = getHistory(playerComponents);
        for (QuestHistoryRecord record : history.getAll()) {
            QuestAsset asset = record.getAsset();

            if (asset == null) {
                history.unregister(record);
                continue;
            }

            if (!asset.isAutoClaim()) continue;

            grantRewards(record, playerComponents);
        }
    }

    /**
     * Claims one completion on the player's request, for quests that are not {@code AutoClaim}.
     *
     * @param questId the id the quest had while it was live
     * @return {@code false} if that completion is unknown or has nothing left to give
     */
    public boolean claimRewards(@Nonnull UUID questId, @Nonnull EntityComponents playerComponents) {
        QuestHistoryRecord record = getHistory(playerComponents).get(questId);
        if (record == null || !record.isClaimable()) return false;

        grantRewards(record, playerComponents);
        return true;
    }

    /**
     * Updates the record after each success, so a reward sees what the previous ones granted.
     */
    private void grantRewards(@Nonnull QuestHistoryRecord record, @Nonnull EntityComponents playerComponents) {
        if (record.isClaimed()) return;

        QuestReward[] pending = record.getPendingRewards();
        List<QuestReward> remaining = new ArrayList<>(Arrays.asList(pending));

        for (QuestReward reward : pending) {
            if (!reward.grant(record, playerComponents)) continue;

            remaining.remove(reward);
            record.setPendingRewards(remaining.toArray(new QuestReward[0]));
        }
    }
}
