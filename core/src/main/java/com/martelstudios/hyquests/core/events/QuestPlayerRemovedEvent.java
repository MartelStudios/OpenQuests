package com.martelstudios.hyquests.core.events;

import com.hypixel.hytale.event.IEvent;
import com.martelstudios.hyquests.core.models.AbstractQuest;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Fired by {@link AbstractQuest#removePlayer}, and for every player when the quest is unregistered.
 * The counterpart of {@link QuestPlayerAddedEvent}: what each scope listens to in order to undo
 * whatever it recorded.
 */
public class QuestPlayerRemovedEvent implements IEvent<UUID> {
    @Nonnull
    private final AbstractQuest<?> quest;
    @Nonnull
    private final UUID playerId;

    public QuestPlayerRemovedEvent(@Nonnull AbstractQuest<?> quest, @Nonnull UUID playerId) {
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
