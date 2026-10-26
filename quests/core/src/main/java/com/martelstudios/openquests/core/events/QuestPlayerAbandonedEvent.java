package com.martelstudios.openquests.core.events;

import com.hypixel.hytale.event.IEvent;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * A player left a quest. Distinct from {@link QuestCompletedEvent}, which is about the quest itself reaching an outcome.
 */
public class QuestPlayerAbandonedEvent implements IEvent<UUID> {

    @Nonnull
    private final AbstractQuestProgression<?> quest;

    @Nonnull
    private final UUID playerId;

    public QuestPlayerAbandonedEvent(@Nonnull AbstractQuestProgression<?> quest, @Nonnull UUID playerId) {
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
