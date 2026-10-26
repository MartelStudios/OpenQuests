package com.martelstudios.openquests.core.stores;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.openquests.core.rewards.stores.PendingRewardStoreComponent;

import javax.annotation.Nonnull;

/**
 * The components OpenQuests keeps on a player, as the entity store registered them.
 *
 * @param questStore the quests the player holds and what they finished
 * @param pendingRewards what the player is owed
 */
public record PlayerComponentTypes(@Nonnull ComponentType<EntityStore, QuestStoreComponent> questStore,
                                   @Nonnull ComponentType<EntityStore, PendingRewardStoreComponent> pendingRewards) {}
