package com.martelstudios.openquests.extension.track.events;

import com.hypixel.hytale.event.IEvent;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Fired when a quest starts being tracked, for everyone holding it. Tracking changes nothing
 * about the quest itself, so nothing else announces it — anything drawing a player's quests has
 * this and only this to go on.
 */
public class QuestTrackedEvent implements IEvent<UUID> {
    @Nonnull
    private final AbstractQuestProgression<?> quest;

    public QuestTrackedEvent(@Nonnull AbstractQuestProgression<?> quest) {
        this.quest = quest;
    }

    @Nonnull
    public AbstractQuestProgression<?> getQuest() {
        return quest;
    }
}
