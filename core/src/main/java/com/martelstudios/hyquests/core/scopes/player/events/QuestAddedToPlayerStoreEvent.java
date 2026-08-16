package com.martelstudios.hyquests.core.scopes.player.events;

import com.hypixel.hytale.event.IEvent;
import com.martelstudios.hyquests.core.events.QuestRegisteredEvent;
import com.martelstudios.hyquests.core.models.AbstractQuest;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Fired when a quest is registered into a specific player's store, regardless of scope (direct
 * player, world, or universe). Unlike {@link QuestRegisteredEvent} (fired once per quest instance),
 * this fires once per (quest, player) pair — the right hook for per-player catch-up logic.
 */
public class QuestAddedToPlayerStoreEvent implements IEvent<UUID> {
    @Nonnull
    private final AbstractQuest<?> quest;
    @Nonnull
    private final UUID playerId;

    public QuestAddedToPlayerStoreEvent(@Nonnull AbstractQuest<?> quest, @Nonnull UUID playerId) {
        this.quest = quest;
        this.playerId = playerId;
    }

    @Nonnull
    public AbstractQuest<?> getQuest() {
        return quest;
    }

    @Nonnull
    public UUID getPlayerId() {
        return playerId;
    }
}
