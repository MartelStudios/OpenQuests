package com.martelstudios.openquests.core.events;

import com.hypixel.hytale.event.IEvent;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Fired by {@link AbstractQuestProgression#addPlayer} once the quest holds the player. Nothing has been
 * written to the player yet — {@link QuestAddedToPlayerStoreEvent} announces that — so this is
 * what any scope listens to in order to record the assignment on its side.
 */
public class QuestPlayerAddedEvent implements IEvent<UUID> {
    @Nonnull
    private final AbstractQuestProgression<?> quest;
    @Nonnull
    private final UUID playerId;

    public QuestPlayerAddedEvent(@Nonnull AbstractQuestProgression<?> quest, @Nonnull UUID playerId) {
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
