package com.martelstudios.openquests.core.events;

import com.hypixel.hytale.event.IEvent;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.QuestState;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Fired once a quest reached a terminal state and left the store. Keyed by quest id, so a
 * listener can watch one specific quest rather than filtering every completion.
 */
public class QuestCompletedEvent implements IEvent<UUID> {
    @Nonnull
    private final AbstractQuestProgression<?> quest;

    @Nonnull
    private final QuestState state;

    public QuestCompletedEvent(@Nonnull AbstractQuestProgression<?> quest) {
        this.quest = quest;
        this.state = quest.getState();
    }

    @Nonnull
    public AbstractQuestProgression<?> getQuest() {
        return quest;
    }

    @Nonnull
    public QuestState getState() {
        return state;
    }
}
