package com.martelstudios.openquests.extension.quests.queststate;

import com.martelstudios.openquests.core.events.QuestLoadedEvent;
import com.martelstudios.openquests.core.events.QuestStateChangedEvent;
import com.martelstudios.openquests.core.events.QuestUnloadedEvent;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.scopes.player.events.QuestAddedToPlayerStoreEvent;
import com.martelstudios.openquests.core.scopes.player.events.QuestRemovedFromPlayerStoreEvent;
import com.martelstudios.openquests.core.services.QuestProgressionService;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * Keeps {@link QuestStateIndex} in step with the quest store, and re-evaluates what a change can
 * actually reach. Watching a quest by asset id rather than by instance is what forces an index:
 * the watched instance may not exist yet, or several may.
 */
public final class QuestStateQuestEvents {

    private QuestStateQuestEvents() {}

    public static void handleQuestLoaded(@Nonnull QuestLoadedEvent event) {
        forEachHolder(event.getQuest(), QuestStateQuestEvents::track);
    }

    public static void handleQuestUnloaded(@Nonnull QuestUnloadedEvent event) {
        forEachHolder(event.getQuest(), QuestStateIndex::forget);
    }

    public static void handleQuestAddedToPlayerStore(@Nonnull QuestAddedToPlayerStoreEvent event) {
        track(event.getQuest(), event.getPlayerId());
    }

    public static void handleQuestRemovedFromPlayerStore(@Nonnull QuestRemovedFromPlayerStoreEvent event) {
        QuestStateIndex.forget(event.getQuest(), event.getPlayerId());
    }

    public static void handleQuestStateChanged(@Nonnull QuestStateChangedEvent event) {
        AbstractQuestProgression<?> quest = event.getQuest();

        for (UUID playerId : quest.getPlayers()) {
            reevaluate(playerId, QuestStateIndex.watchersOf(quest.getAssetId(), playerId));
        }
    }

    /**
     * A quest arriving can satisfy a watcher, and can be one that has yet to be evaluated at all.
     * Order is therefore free: a watcher that arrives before its candidates answers on what is
     * there, and each candidate arriving after puts the answer right.
     */
    private static void track(@Nonnull AbstractQuestProgression<?> quest, @Nonnull UUID playerId) {
        QuestStateIndex.track(quest, playerId);

        Set<UUID> watcherIds = new HashSet<>(QuestStateIndex.watchersOf(quest.getAssetId(), playerId));
        if (quest instanceof QuestStateQuestProgression) watcherIds.add(quest.getId());

        reevaluate(playerId, watcherIds);
    }

    /**
     * Those who gave the quest up count too: it stays in their store, and reading it is the whole
     * point of an {@code ABANDONED} requirement.
     */
    private static void forEachHolder(@Nonnull AbstractQuestProgression<?> quest, @Nonnull BiConsumer<AbstractQuestProgression<?>, UUID> action) {
        quest.getPlayers().forEach(playerId -> action.accept(quest, playerId));
        quest.getAbandonedPlayers().forEach(playerId -> action.accept(quest, playerId));
    }

    private static void reevaluate(@Nonnull UUID playerId, @Nonnull Collection<UUID> watcherIds) {
        if (watcherIds.isEmpty()) return;

        QuestProgressionService.get().progress(new QuestStateQuestVisitor(playerId), watcherIds);
    }
}
