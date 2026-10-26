package com.martelstudios.openquests.core.events;

import com.hypixel.hytale.event.IEvent;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Fired from {@link AbstractQuestProgression#update} when a visitor leaves the quest dirty. Lets other
 * systems (prerequisite re-checks, UI refresh, ...) react without {@link AbstractQuestProgression} needing
 * to know about them.
 */
public class QuestUpdatedEvent implements IEvent<UUID> {
    @Nonnull
    private final AbstractQuestProgression<?> quest;

    public QuestUpdatedEvent(@Nonnull AbstractQuestProgression<?> quest) {
        this.quest = quest;
    }

    @Nonnull
    public AbstractQuestProgression<?> getQuest() {
        return quest;
    }
}
