package com.martelstudios.hyquests.events;

import com.hypixel.hytale.event.IEvent;
import com.martelstudios.hyquests.models.AbstractQuest;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Fired when a quest is registered into a specific player's store, regardless of scope (direct
 * player, world, or universe). Unlike {@link QuestRegisteredEvent} (fired once per quest instance),
 * this fires once per (quest, player) pair — the right hook for per-player catch-up logic.
 */
public class QuestAssignedToPlayerEvent implements IEvent<UUID> {
    @Nonnull
    private final AbstractQuest<?> quest;
    @Nonnull
    private final UUID playerId;

    public QuestAssignedToPlayerEvent(@Nonnull AbstractQuest<?> quest, @Nonnull UUID playerId) {
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
