package com.martelstudios.openquests.core.rewards.services;

import com.hypixel.hytale.event.EventPriority;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.martelstudios.openquests.core.OpenQuestsCorePlugin;
import com.martelstudios.openquests.core.events.QuestCompletedEvent;
import com.martelstudios.openquests.core.events.QuestPlayerAbandonedEvent;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.QuestAsset;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.rewards.QuestReward;
import com.martelstudios.openquests.core.rewards.models.PendingRewards;
import com.martelstudios.openquests.core.rewards.stores.PendingRewardStore;
import com.martelstudios.openquests.core.rewards.stores.PendingRewardStoreComponent;
import com.martelstudios.openquests.core.utils.EntityComponents;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

/**
 * What a quest pays, and to whom. Owns the debt rather than the quest does: a quest is shared by
 * everyone holding it, so what each of them is still owed is theirs alone.
 *
 * <p>A reward only ever reaches a player who is there to receive it. One who is offline keeps the
 * debt standing, collected on their next world entry.
 */
public class QuestRewardService {

    public QuestRewardService(JavaPlugin plugin) {
        plugin.getEventRegistry().registerGlobal(AddPlayerToWorldEvent.class, this::handleAddPlayerToWorldEvent);
        // EventPriority.FIRST, to grant rewards earlier
        plugin.getEventRegistry()
              .registerGlobal(EventPriority.FIRST, QuestCompletedEvent.class, this::handleQuestCompletedEvent);
        plugin.getEventRegistry()
              .registerGlobal(EventPriority.FIRST, QuestPlayerAbandonedEvent.class, this::handleQuestPlayerAbandonedEvent);
    }

    public static QuestRewardService get() {
        return OpenQuestsCorePlugin.get().getQuestRewardService();
    }

    /**
     * @return what a player is still owed, created if they are owed nothing yet.
     */
    @Nonnull
    public PendingRewardStore getPending(@Nonnull EntityComponents playerComponents) {
        return playerComponents.ensureAndGetComponent(PendingRewardStoreComponent.getComponentType()).pending;
    }

    /**
     * @return whether that completion still has anything to hand over to this player.
     */
    public boolean isOwed(@Nonnull UUID questId, @Nonnull EntityComponents playerComponents) {
        return getPending(playerComponents).isOwed(questId);
    }

    /**
     * Writes down what the outcome owes each player holding the quest, and hands it over at once
     * for a quest that claims itself.
     */
    private void handleQuestCompletedEvent(QuestCompletedEvent questCompletedEvent) {
        AbstractQuestProgression<?> quest = questCompletedEvent.getQuest();

        QuestAsset asset = quest.getAsset();
        if (asset == null) return;

        QuestReward[] rewards = asset.getRewards(questCompletedEvent.getState());
        if (rewards.length == 0) return;

        for (UUID playerId : quest.getPlayers()) {
            EntityComponents.update(playerId, components -> {
                var pending = new PendingRewards(quest, rewards);

                getPending(components).add(pending);

                grantAuto(pending, components);
            });
        }
    }

    /**
     * Pays one player for giving up, at the moment they do.
     */
    private void handleQuestPlayerAbandonedEvent(QuestPlayerAbandonedEvent questPlayerAbandonedEvent) {
        AbstractQuestProgression<?> quest = questPlayerAbandonedEvent.getQuest();

        QuestAsset asset = quest.getAsset();
        if (asset == null) return;

        QuestReward[] rewards = asset.getRewards(QuestState.ABANDONED);
        if (rewards.length == 0) return;

        EntityComponents.update(questPlayerAbandonedEvent.getPlayerId(), components -> {
            var pending = new PendingRewards(quest, rewards);

            getPending(components).add(pending);

            grantAuto(pending, components);
        });
    }

    /**
     * On world entry rather than on connection, so the player is there to be shown the reward.
     */
    private void handleAddPlayerToWorldEvent(@Nonnull AddPlayerToWorldEvent addPlayerToWorldEvent) {
        var playerRef = addPlayerToWorldEvent.getHolder().getComponent(PlayerRef.getComponentType());
        if (playerRef == null || playerRef.getReference() == null) return;

        claimAuto(EntityComponents.of(playerRef.getReference()));
    }

    /**
     * Hands over everything standing that the quest claims on the player's behalf. What is left
     * behind is what a quest expects them to come and collect.
     */
    public void claimAuto(@Nonnull EntityComponents playerComponents) {
        PendingRewardStore store = getPending(playerComponents);

        for (PendingRewards pending : store.getAll()) {
            grantAuto(pending, playerComponents);
        }
    }

    /**
     * Collects one completion on the player's asking, for quests that are not {@code AutoClaim}.
     *
     * @param questId the id the quest had while it was live
     * @return {@code false} if that completion owes this player nothing
     */
    public boolean claim(@Nonnull UUID questId, @Nonnull EntityComponents playerComponents) {
        PendingRewards pending = getPending(playerComponents).get(questId);
        if (pending == null) return false;

        grant(pending, playerComponents);
        return true;
    }

    private void grantAuto(@Nonnull PendingRewards pending, @Nonnull EntityComponents playerComponents) {
        QuestReward[] rewards = pending.getRewards();
        var remaining = new ArrayList<>(Arrays.asList(rewards));

        for (QuestReward reward : rewards) {
            if (!reward.isAutoClaim()) continue;

            if (!reward.grant(pending.getQuestId(), playerComponents)) continue;

            remaining.remove(reward);
            pending.setRewards(remaining.toArray(PendingRewards.NO_REWARDS));
        }

        if (pending.isEmpty()) getPending(playerComponents).remove(pending.getQuestId());
    }

    /**
     * Hands over what is owed, one reward at a time, writing down what is left after each. A
     * reward that cannot be granted right now — a full inventory — stays owed and is retried.
     */
    private void grant(@Nonnull PendingRewards pending, @Nonnull EntityComponents playerComponents) {
        QuestReward[] rewards = pending.getRewards();
        var remaining = new ArrayList<>(Arrays.asList(rewards));

        for (QuestReward reward : rewards) {
            if (!reward.grant(pending.getQuestId(), playerComponents)) continue;

            remaining.remove(reward);
            pending.setRewards(remaining.toArray(PendingRewards.NO_REWARDS));
        }

        if (pending.isEmpty()) getPending(playerComponents).remove(pending.getQuestId());
    }
}
