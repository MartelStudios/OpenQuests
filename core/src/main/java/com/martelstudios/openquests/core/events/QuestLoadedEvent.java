package com.martelstudios.openquests.core.events;

import com.hypixel.hytale.event.IEvent;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Fired when the store starts holding a quest — created, read back from a player's file, or pulled
 * in by a lookup. {@link QuestRegisteredEvent} is a quest coming into existence; this one is it
 * being in memory, which is what anything indexing quests is waiting for.
 */
public class QuestLoadedEvent implements IEvent<UUID> {
    @Nonnull
    private final AbstractQuestProgression<?> quest;

    public QuestLoadedEvent(@Nonnull AbstractQuestProgression<?> quest) {
        this.quest = quest;
    }

    @Nonnull
    public AbstractQuestProgression<?> getQuest() {
        return quest;
    }
}
