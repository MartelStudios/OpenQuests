package com.martelstudios.openquests.core.events;

import com.hypixel.hytale.event.IEvent;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.QuestState;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Fired once a quest reached a terminal state and left the store. Keyed by quest id, so a
 * listener can watch one specific quest rather than filtering every completion.
 *
 * <p>Every server holding the quest hears it, but only the one whose claim ended it pays what the
 * end pays: a listener with an effect that must happen once checks {@link #isClaimedHere()}.
 */
public class QuestCompletedEvent implements IEvent<UUID> {
    @Nonnull
    private final AbstractQuestProgression<?> quest;

    @Nonnull
    private final QuestState state;

    private final boolean claimedHere;

    public QuestCompletedEvent(@Nonnull AbstractQuestProgression<?> quest) {
        this(quest, true);
    }

    /**
     * @param claimedHere whether this server ended the quest, rather than learning another did
     */
    public QuestCompletedEvent(@Nonnull AbstractQuestProgression<?> quest, boolean claimedHere) {
        this.quest = quest;
        this.state = quest.getState();
        this.claimedHere = claimedHere;
    }

    @Nonnull
    public AbstractQuestProgression<?> getQuest() {
        return quest;
    }

    @Nonnull
    public QuestState getState() {
        return state;
    }

    /**
     * @return whether this server ended the quest and pays what its end pays: rewards, records,
     * whatever must happen once. {@code false} on a server only learning of it, which files the
     * quest away and tells its own players.
     */
    public boolean isClaimedHere() {
        return claimedHere;
    }
}
