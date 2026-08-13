package com.martelstudios.hyquests.events;

import com.hypixel.hytale.event.IEvent;
import com.martelstudios.hyquests.models.AbstractQuest;
import com.martelstudios.hyquests.stores.QuestProgressionStore;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Fired once a quest instance has been added to the {@link QuestProgressionStore}.
 */
public class QuestRegisteredEvent implements IEvent<UUID> {
    @Nonnull
    private final AbstractQuest<?> quest;

    public QuestRegisteredEvent(@Nonnull AbstractQuest<?> quest) {
        this.quest = quest;
    }

    @Nonnull
    public AbstractQuest<?> getQuest() {
        return quest;
    }
}
