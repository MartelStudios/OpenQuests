package com.martelstudios.openquests.core.events;

import com.hypixel.hytale.event.IEvent;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.QuestState;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Fired when a quest moves from one state to another, terminal or not. Narrower than
 * {@link QuestUpdatedEvent}, which also fires on progress that leaves the outcome alone, and wider
 * than {@link QuestCompletedEvent}, which only covers the transitions that end a quest.
 */
public class QuestStateChangedEvent implements IEvent<UUID> {
    @Nonnull
    private final AbstractQuestProgression<?> quest;

    @Nonnull
    private final QuestState previousState;

    @Nonnull
    private final QuestState state;

    private final int transitions;

    /**
     * @param state the state reached, which the quest may have moved past by the time this is heard
     * @param transitions how many changes of state the quest went through, this one included
     */
    public QuestStateChangedEvent(@Nonnull AbstractQuestProgression<?> quest, @Nonnull QuestState previousState, @Nonnull QuestState state, int transitions) {
        this.quest = quest;
        this.previousState = previousState;
        this.state = state;
        this.transitions = transitions;
    }

    @Nonnull
    public AbstractQuestProgression<?> getQuest() {
        return quest;
    }

    /**
     * @return the state the quest held before this change, never equal to {@link #getState()}.
     */
    @Nonnull
    public QuestState getPreviousState() {
        return previousState;
    }

    @Nonnull
    public QuestState getState() {
        return state;
    }

    /**
     * @return how many changes of state the quest went through, this one included: what tells
     * older news of the quest from newer, heard in whatever order.
     */
    public int getTransitions() {
        return transitions;
    }
}
