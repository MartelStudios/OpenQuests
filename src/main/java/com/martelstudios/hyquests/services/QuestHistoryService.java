package com.martelstudios.hyquests.services;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.hyquests.HyQuestsPlugin;
import com.martelstudios.hyquests.assets.QuestAsset;
import com.martelstudios.hyquests.events.QuestCompletedEvent;
import com.martelstudios.hyquests.models.AbstractQuest;
import com.martelstudios.hyquests.models.QuestHistoryRecord;
import com.martelstudios.hyquests.rewards.QuestReward;
import com.martelstudios.hyquests.stores.QuestHistoryStoreComponent;

import javax.annotation.Nonnull;
import java.util.UUID;

public class QuestHistoryService {
    public static QuestHistoryService get() {
        return HyQuestsPlugin.get().getQuestHistoryService();
    }

    public void handleQuestCompletedEvent(QuestCompletedEvent questCompletedEvent) {
        var quest = questCompletedEvent.getQuest();
        for (UUID playerId : quest.getPlayers()) {
            archiveQuestForPlayer(quest, playerId);
        }
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
            playerRef.getStore().ensureAndGetComponent(playerRef, QuestHistoryStoreComponent.getComponentType()).history.register(record);

            if (autoClaim) {
                grantRewards(record, playerRef);
            }
        });
    }

    private void archiveQuestForOfflinePlayer(@Nonnull AbstractQuest<?> quest, @Nonnull UUID playerId) {
        var record = new QuestHistoryRecord(quest);
        Universe.get()
                .getPlayerStorage()
                .update(playerId, holder -> holder.ensureAndGetComponent(QuestHistoryStoreComponent.getComponentType()).history.register(record));
    }

    private void grantRewards(@Nonnull QuestHistoryRecord record, @Nonnull Ref<EntityStore> playerRef) {
        if (!record.isClaimed()) return;

        if (!record.isClaimable()) {
            record.setClaimed(true);
            return;
        }

        var asset = record.getAsset();
        var rewards = asset.getRewards(record.getState());
        for (QuestReward reward : rewards) {
            reward.grant(asset, playerRef);
        }
        record.setClaimed(true);
    }

    /**
     * Grants what a player earned while they were away. Called once they are connected, since
     * completions that happened offline could not hand their rewards over at the time.
     */
    public void claimAutoRewards(@Nonnull Ref<EntityStore> playerRef) {
        var history = playerRef.getStore()
                               .ensureAndGetComponent(playerRef, QuestHistoryStoreComponent.getComponentType()).history;

        for (QuestHistoryRecord record : history.getAll()) {
            QuestAsset asset = record.getAsset();
            if (asset == null) {
                record.setClaimed(true);
                continue;
            }

            if (!asset.isAutoClaim()) continue;

            grantRewards(record, playerRef);
        }
    }
}
