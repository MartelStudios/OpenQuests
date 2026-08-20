package com.martelstudios.hyquests.core.events;

import com.hypixel.hytale.event.IEvent;
import com.martelstudios.hyquests.core.models.AbstractQuestProgression;
import com.martelstudios.hyquests.core.stores.QuestProgressionStore;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Fired once a quest instance has been added to the {@link QuestProgressionStore}.
 */
public class QuestRegisteredEvent implements IEvent<UUID> {
    @Nonnull
    private final AbstractQuestProgression<?> quest;

    public QuestRegisteredEvent(@Nonnull AbstractQuestProgression<?> quest) {
        this.quest = quest;
    }

    @Nonnull
    public AbstractQuestProgression<?> getQuest() {
        return quest;
    }
}
