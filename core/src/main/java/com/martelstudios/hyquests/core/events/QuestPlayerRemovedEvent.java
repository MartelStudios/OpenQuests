package com.martelstudios.hyquests.core.events;

import com.hypixel.hytale.event.IEvent;
import com.martelstudios.hyquests.core.models.AbstractQuestProgression;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Fired by {@link AbstractQuestProgression#removePlayer}, and for every player when the quest is unregistered.
 * The counterpart of {@link QuestPlayerAddedEvent}: what each scope listens to in order to undo
 * whatever it recorded.
 */
public class QuestPlayerRemovedEvent implements IEvent<UUID> {
    @Nonnull
    private final AbstractQuestProgression<?> quest;
    @Nonnull
    private final UUID playerId;

    public QuestPlayerRemovedEvent(@Nonnull AbstractQuestProgression<?> quest, @Nonnull UUID playerId) {
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
