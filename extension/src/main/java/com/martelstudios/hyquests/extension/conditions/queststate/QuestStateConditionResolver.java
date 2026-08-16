package com.martelstudios.hyquests.extension.conditions.queststate;

import com.martelstudios.hyquests.core.assignment.assets.QuestAssignmentAsset;
import com.martelstudios.hyquests.core.assignment.conditions.QuestAssignmentConditionResolver;
import com.martelstudios.hyquests.core.assignment.services.QuestAssignmentService;
import com.martelstudios.hyquests.core.events.QuestCompletedEvent;
import com.martelstudios.hyquests.core.history.models.QuestHistoryRecord;
import com.martelstudios.hyquests.core.history.stores.QuestHistoryStoreComponent;
import com.martelstudios.hyquests.core.models.QuestState;
import com.martelstudios.hyquests.core.services.QuestProgressionService;
import com.martelstudios.hyquests.core.stores.QuestStoreComponent;
import com.martelstudios.hyquests.core.utils.EntityComponents;
import com.martelstudios.hyquests.extension.conditions.queststate.QuestStateCondition.QuestStateRequirement;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * Indexes {@link QuestStateCondition}s by the quest asset they watch, so completing a quest only
 * touches the assignments that actually mention it.
 */
public class QuestStateConditionResolver implements QuestAssignmentConditionResolver<QuestStateCondition> {

    private final Map<String, List<Listener>> listeners = new HashMap<>();

    @Override
    public void register(@Nonnull QuestAssignmentAsset asset, @Nonnull QuestStateCondition condition) {
        listeners.computeIfAbsent(condition.getQuestAssetId(), _ -> new ArrayList<>())
                 .add(new Listener(asset, condition));
    }

    /**
     * A live quest is necessarily in progress, so a terminal requirement only ever reads the
     * history. {@code STARTED} spans both: the quest exists, whatever became of it.
     */
    @Override
    public boolean evaluate(QuestStateCondition stateCondition, @Nonnull UUID playerId) {
        var components = EntityComponents.of(playerId);
        var stateRequirement = stateCondition.questStateRequirement;
        var questAssetId = stateCondition.questAssetId;

        if (stateRequirement == QuestStateRequirement.STARTED || stateRequirement == QuestStateRequirement.IN_PROGRESS) {
            var questStore = components.getComponent(QuestStoreComponent.getComponentType());
            if (questStore != null) {
                for (UUID questId : questStore.questsRecord.getAllIds()) {
                    var quest = QuestProgressionService.get().getQuest(questId);
                    if (quest != null && questAssetId.equals(quest.getAssetId()) && matches(stateCondition, quest.getState())) {
                        return true;
                    }
                }
            }

            if (stateRequirement == QuestStateRequirement.IN_PROGRESS) return false;
        }

        var historyStore = components.getComponent(QuestHistoryStoreComponent.getComponentType());
        if (historyStore == null) return false;

        for (QuestHistoryRecord record : historyStore.history.getForAsset(questAssetId)) {
            if (matches(stateCondition, record.getState())) return true;
        }

        return false;
    }

    public void handleQuestCompletedEvent(@Nonnull QuestCompletedEvent questCompletedEvent) {
        var quest = questCompletedEvent.getQuest();
        List<Listener> listening = listeners.get(quest.getAssetId());
        if (listening == null) return;

        for (Listener listener : listening) {
            if (!matches(listener.condition(), quest.getState())) continue;

            for (UUID playerId : quest.getPlayers()) {
                QuestAssignmentService.get().completeAssignmentCondition(listener.asset(), listener.condition(), playerId);
            }
        }
    }

    private boolean matches(QuestStateCondition stateCondition, @Nonnull QuestState state) {
        return switch (stateCondition.questStateRequirement) {
            case STARTED -> true;
            case IN_PROGRESS -> state == QuestState.IN_PROGRESS;
            case COMPLETED -> state != QuestState.IN_PROGRESS;
            case SUCCESSFULLY -> state == QuestState.SUCCESSFUL;
            case FAILED -> state == QuestState.FAILED;
            case ABANDONED -> state == QuestState.ABANDONED;
        };
    }

    private record Listener(@Nonnull QuestAssignmentAsset asset, @Nonnull QuestStateCondition condition) {}
}
