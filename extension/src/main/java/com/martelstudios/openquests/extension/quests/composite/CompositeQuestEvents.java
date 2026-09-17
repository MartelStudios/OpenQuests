package com.martelstudios.openquests.extension.quests.composite;

import com.martelstudios.openquests.core.events.QuestLoadedEvent;
import com.martelstudios.openquests.core.events.QuestUnloadedEvent;

import javax.annotation.Nonnull;

/**
 * Hands a group its children as it enters the store and takes them back as it leaves. Tied to the
 * store rather than to decoding, which is what makes a group heard exactly once: the store turns
 * away a second copy of an id it already holds, and never announces it.
 */
public final class CompositeQuestEvents {

    private CompositeQuestEvents() {}

    public static void handleQuestLoaded(@Nonnull QuestLoadedEvent event) {
        if (event.getQuest() instanceof CompositeQuestProgression composite) composite.listenToChildren();
    }

    public static void handleQuestUnloaded(@Nonnull QuestUnloadedEvent event) {
        if (event.getQuest() instanceof CompositeQuestProgression composite) composite.stopListeningToChildren();
    }
}
