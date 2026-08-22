package com.martelstudios.openquests.extension.quests.queststate;

import com.martelstudios.openquests.core.events.QuestCompletedEvent;
import com.martelstudios.openquests.core.events.QuestUpdatedEvent;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.scopes.player.events.QuestAddedToPlayerStoreEvent;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.core.stores.QuestStoreComponent;
import com.martelstudios.openquests.core.utils.EntityComponents;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Re-evaluates a player's quest-state quests whenever one of their quests changes. Watching the
 * quest by asset id rather than by instance is what forces this: the watched instance may not
 * exist yet, or several may.
 */
public final class QuestStateQuestEvents {

    private QuestStateQuestEvents() {}

    public static void handleQuestAddedToPlayerStore(@Nonnull QuestAddedToPlayerStoreEvent event) {
        reevaluate(event.getPlayerId());
    }

    public static void handleQuestUpdated(@Nonnull QuestUpdatedEvent event) {
        reevaluateFor(event.getQuest());
    }

    public static void handleQuestCompleted(@Nonnull QuestCompletedEvent event) {
        reevaluateFor(event.getQuest());
    }

    private static void reevaluateFor(@Nonnull AbstractQuestProgression<?> quest) {
        if (quest instanceof QuestStateQuestProgression) return;

        for (UUID playerId : quest.getPlayers()) {
            reevaluate(playerId);
        }
    }

    private static void reevaluate(@Nonnull UUID playerId) {
        var questStore = EntityComponents.of(playerId).getComponent(QuestStoreComponent.getComponentType());
        if (questStore == null) return;

        QuestProgressionService.get()
                               .progress(new QuestStateQuestVisitor(playerId), questStore.getQuestIds());
    }
}
