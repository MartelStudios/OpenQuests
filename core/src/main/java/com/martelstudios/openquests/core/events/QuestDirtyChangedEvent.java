package com.martelstudios.openquests.core.events;

import com.hypixel.hytale.event.IEvent;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;

import javax.annotation.Nonnull;
import java.util.UUID;

public class QuestDirtyChangedEvent implements IEvent<UUID> {
    @Nonnull
    private final AbstractQuestProgression<?> quest;

    public QuestDirtyChangedEvent(@Nonnull AbstractQuestProgression<?> quest) {
        this.quest = quest;
    }

    @Nonnull
    public AbstractQuestProgression<?> getQuest() {
        return quest;
    }
}
