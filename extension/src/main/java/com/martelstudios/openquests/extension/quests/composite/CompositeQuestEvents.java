package com.martelstudios.openquests.extension.quests.composite;

import com.martelstudios.openquests.core.events.QuestLoadedEvent;
import com.martelstudios.openquests.core.events.QuestUnloadedEvent;
import com.martelstudios.openquests.core.services.QuestProgressionService;

import javax.annotation.Nonnull;
import java.util.Arrays;

/**
 * Hands a group its children as it enters the store and takes them back as it leaves. Tied to the
 * store rather than to decoding, which is what makes a group heard exactly once: the store turns
 * away a second copy of an id it already holds, and never announces it.
 */
public final class CompositeQuestEvents {

    private CompositeQuestEvents() {}

    /**
     * A group listens to its children for as long as it is in the store. A running one brings
     * them in with it, so a player joining it, or a server following its index, reaches them too,
     * and writes down where they stand rather than what its replicas remembered.
     */
    public static void handleQuestLoaded(@Nonnull QuestLoadedEvent event) {
        if (!(event.getQuest() instanceof CompositeQuestProgression composite)) return;

        if (!composite.isCompleted()) {
            QuestProgressionService.get().loadQuests(Arrays.asList(composite.getChildIds()));
            if (composite.reconcileOutcomes()) composite.markDirty();
        }
        composite.listenToChildren();
    }

    public static void handleQuestUnloaded(@Nonnull QuestUnloadedEvent event) {
        if (event.getQuest() instanceof CompositeQuestProgression composite) composite.stopListeningToChildren();
    }
}
