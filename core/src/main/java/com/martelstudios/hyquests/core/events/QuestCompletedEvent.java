package com.martelstudios.hyquests.core.events;

import com.hypixel.hytale.event.IEvent;
import com.martelstudios.hyquests.core.models.AbstractQuest;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Fired once a quest reached a terminal state and left the store. Keyed by quest id, so a
 * listener can watch one specific quest rather than filtering every completion.
 */
public class QuestCompletedEvent implements IEvent<UUID> {
    @Nonnull
    private final AbstractQuest<?> quest;

    public QuestCompletedEvent(@Nonnull AbstractQuest<?> quest) {
        this.quest = quest;
    }

    @Nonnull
    public AbstractQuest<?> getQuest() {
        return quest;
    }
}
