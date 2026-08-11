package com.martelstudios.hyquests.events;

import com.hypixel.hytale.event.IEvent;
import com.martelstudios.hyquests.models.AbstractQuest;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Fired from {@link AbstractQuest#update} when a visitor leaves the quest dirty. Lets other
 * systems (prerequisite re-checks, UI refresh, ...) react without {@link AbstractQuest} needing
 * to know about them.
 */
public class QuestUpdatedEvent implements IEvent<UUID> {
    @Nonnull
    private final AbstractQuest<?> quest;

    public QuestUpdatedEvent(@Nonnull AbstractQuest<?> quest) {
        this.quest = quest;
    }

    @Nonnull
    public AbstractQuest<?> getQuest() {
        return quest;
    }
}
