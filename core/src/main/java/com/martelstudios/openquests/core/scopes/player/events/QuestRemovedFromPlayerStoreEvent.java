package com.martelstudios.openquests.core.scopes.player.events;

import com.hypixel.hytale.event.IEvent;
import com.martelstudios.openquests.core.events.QuestUnregisteredEvent;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Fired when a quest leaves a specific player's store, whatever the scope it came from. Fires
 * once per (quest, player) pair, unlike {@link QuestUnregisteredEvent} which fires once for the
 * whole instance.
 */
public class QuestRemovedFromPlayerStoreEvent implements IEvent<UUID> {
    @Nonnull
    private final AbstractQuestProgression<?> quest;
    @Nonnull
    private final UUID playerId;

    public QuestRemovedFromPlayerStoreEvent(@Nonnull AbstractQuestProgression<?> quest, @Nonnull UUID playerId) {
        this.quest = quest;
        this.playerId = playerId;
    }

    @Nonnull
    public AbstractQuestProgression<?> getQuest() {
        return quest;
    }

    @Nonnull
    public UUID getPlayerId() {
        return playerId;
    }
}
