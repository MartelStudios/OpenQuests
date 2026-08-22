package com.martelstudios.openquests.extension.quests.queststate;

import com.martelstudios.openquests.core.history.stores.QuestHistoryStoreComponent;
import com.martelstudios.openquests.core.history.models.QuestHistoryRecord;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.core.stores.QuestStoreComponent;
import com.martelstudios.openquests.core.utils.EntityComponents;
import com.martelstudios.openquests.core.visitors.QuestVisitor;

import javax.annotation.Nonnull;
import java.util.UUID;

public class QuestStateQuestVisitor implements QuestVisitor<QuestStateQuestProgression> {

    private final UUID playerId;

    public QuestStateQuestVisitor(@Nonnull UUID playerId) {
        this.playerId = playerId;
    }

    @Override
    public void progress(QuestStateQuestProgression quest) {
        if (!quest.getPlayers().contains(playerId)) return;
        if (quest.isCompleted() && quest.isStopOnComplete()) return;

        boolean satisfied = quest.isNot() != matches(quest);

        quest.setState(satisfied ? QuestState.SUCCESSFUL : QuestState.IN_PROGRESS).markDirty();
    }

    /**
     * Live quests first, then the history: a quest that stopped no longer has an instance.
     */
    private boolean matches(@Nonnull QuestStateQuestProgression quest) {
        var components = EntityComponents.of(playerId);
        String watchedAssetId = quest.getQuestAssetId();

        var questStore = components.getComponent(QuestStoreComponent.getComponentType());
        if (questStore != null) {
            for (UUID questId : questStore.getQuestIds()) {
                var watched = QuestProgressionService.get().getQuest(questId);
                if (watched != null && watchedAssetId.equals(watched.getAssetId()) && matchesState(quest, watched.getState())) {
                    return true;
                }
            }
        }

        var historyStore = components.getComponent(QuestHistoryStoreComponent.getComponentType());
        if (historyStore == null) return false;

        for (QuestHistoryRecord record : historyStore.history.getForAsset(watchedAssetId)) {
            if (matchesState(quest, record.getState())) return true;
        }

        return false;
    }

    private boolean matchesState(@Nonnull QuestStateQuestProgression quest, @Nonnull QuestState state) {
        return switch (quest.getQuestStateRequirement()) {
            case STARTED -> true;
            case IN_PROGRESS -> state == QuestState.IN_PROGRESS;
            case COMPLETED -> state != QuestState.IN_PROGRESS;
            case SUCCESSFULLY -> state == QuestState.SUCCESSFUL;
            case FAILED -> state == QuestState.FAILED;
            case ABANDONED -> state == QuestState.ABANDONED;
        };
    }

    @Override
    public Class<QuestStateQuestProgression> getQuestType() {
        return QuestStateQuestProgression.class;
    }
}
