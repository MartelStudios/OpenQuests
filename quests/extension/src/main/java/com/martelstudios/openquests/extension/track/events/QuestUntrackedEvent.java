package com.martelstudios.openquests.extension.track.events;

import com.hypixel.hytale.event.IEvent;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Fired when a player stops tracking a quest. Tracking changes nothing about the quest itself, so
 * nothing else announces it: anything drawing a player's quests has this and only this to go on.
 */
public class QuestUntrackedEvent implements IEvent<UUID> {
    @Nonnull
    private final AbstractQuestProgression<?> quest;

    @Nonnull
    private final UUID playerId;

    public QuestUntrackedEvent(@Nonnull AbstractQuestProgression<?> quest, @Nonnull UUID playerId) {
        this.quest = quest;
        this.playerId = playerId;
    }

    @Nonnull
    public AbstractQuestProgression<?> getQuest() {
        return quest;
    }

    /**
     * @return the player whose tracker this is about: the quest's other holders keep theirs.
     */
    @Nonnull
    public UUID getPlayerId() {
        return playerId;
    }
}
