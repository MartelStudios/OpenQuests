package com.martelstudios.hyquests.core.scopes.player.events;

import com.hypixel.hytale.event.IEvent;
import com.martelstudios.hyquests.core.events.QuestUnregisteredEvent;
import com.martelstudios.hyquests.core.models.AbstractQuest;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Fired when a quest leaves a specific player's store, whatever the scope it came from. Fires
 * once per (quest, player) pair, unlike {@link QuestUnregisteredEvent} which fires once for the
 * whole instance.
 */
public class QuestRemovedFromPlayerStoreEvent implements IEvent<UUID> {
    @Nonnull
    private final AbstractQuest<?> quest;
    @Nonnull
    private final UUID playerId;

    public QuestRemovedFromPlayerStoreEvent(@Nonnull AbstractQuest<?> quest, @Nonnull UUID playerId) {
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
