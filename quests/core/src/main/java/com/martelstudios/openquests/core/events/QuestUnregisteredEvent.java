package com.martelstudios.openquests.core.events;

import com.hypixel.hytale.event.IEvent;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.stores.QuestProgressionStore;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Fired once a quest instance has been added to the {@link QuestProgressionStore}.
 */
public class QuestUnregisteredEvent implements IEvent<UUID> {
    @Nonnull
    private final AbstractQuestProgression<?> quest;

    public QuestUnregisteredEvent(@Nonnull AbstractQuestProgression<?> quest) {
        this.quest = quest;
    }

    @Nonnull
    public AbstractQuestProgression<?> getQuest() {
        return quest;
    }
}
