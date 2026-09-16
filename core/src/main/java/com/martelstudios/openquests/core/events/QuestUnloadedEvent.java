package com.martelstudios.openquests.core.events;

import com.hypixel.hytale.event.IEvent;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Fired when the store stops holding a quest that still exists on disk, the mirror of
 * {@link QuestLoadedEvent}. A quest done away with fires {@link QuestUnregisteredEvent} instead,
 * and the two must not be treated alike: this one comes back on the next lookup by id.
 */
public class QuestUnloadedEvent implements IEvent<UUID> {
    @Nonnull
    private final AbstractQuestProgression<?> quest;

    public QuestUnloadedEvent(@Nonnull AbstractQuestProgression<?> quest) {
        this.quest = quest;
    }

    @Nonnull
    public AbstractQuestProgression<?> getQuest() {
        return quest;
    }
}
