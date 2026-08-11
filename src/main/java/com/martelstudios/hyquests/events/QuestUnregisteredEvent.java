package com.martelstudios.hyquests.events;

import com.hypixel.hytale.event.IEvent;
import com.martelstudios.hyquests.models.AbstractQuest;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Fired once a quest instance has been added to the {@link com.martelstudios.hyquests.stores.QuestDataStore}.
 */
public class QuestUnregisteredEvent implements IEvent<UUID> {
    @Nonnull
    private final AbstractQuest<?> quest;

    public QuestUnregisteredEvent(@Nonnull AbstractQuest<?> quest) {
        this.quest = quest;
    }

    @Nonnull
    public AbstractQuest<?> getQuest() {
        return quest;
    }
}
