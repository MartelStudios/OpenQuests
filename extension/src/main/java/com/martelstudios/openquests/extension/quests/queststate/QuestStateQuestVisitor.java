package com.martelstudios.openquests.extension.quests.queststate;

import com.martelstudios.openquests.core.models.QuestOperation;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.services.QuestProgressionService;
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
        if (quest.isOver()) return;

        QuestState answer = quest.isNot() != matches(quest) ? QuestState.SUCCESSFUL : QuestState.IN_PROGRESS;

        quest.apply(new QuestOperation.SetState(answer));
    }

    /**
     * Any of them, running or finished alike: a quest that ended is set aside rather than deleted,
     * so it answers here the same way it did while it was running. Read as this player sees it,
     * since one who left a shared quest has abandoned it while it runs on for the others.
     */
    private boolean matches(@Nonnull QuestStateQuestProgression quest) {
        for (UUID questId : QuestStateIndex.get().candidatesOf(quest.getQuestAssetId(), playerId)) {
            var watched = QuestProgressionService.get().getQuest(questId);

            if (watched != null && matchesState(quest, watched.getStateFor(playerId))) return true;
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
