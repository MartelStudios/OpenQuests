package com.martelstudios.openquests.extension.quests.gameflag;

import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.visitors.QuestVisitor;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.function.ToIntFunction;

/**
 * Completes the player's game flag quests whose flag reached its level. A flag lowered afterwards
 * never reopens a quest already over.
 */
public class GameFlagQuestVisitor implements QuestVisitor<GameFlagQuestProgression> {

    private final UUID playerId;
    private final ToIntFunction<String> levels;

    public GameFlagQuestVisitor(@Nonnull UUID playerId, @Nonnull ToIntFunction<String> levels) {
        this.playerId = playerId;
        this.levels = levels;
    }

    @Override
    public void progress(GameFlagQuestProgression quest) {
        if (!quest.getPlayers().contains(playerId)) return;
        if (quest.isOver()) return;
        if (!quest.getAsset().isReached(levels)) return;

        quest.apply(new AbstractQuestProgression.SetState(QuestState.SUCCESSFUL));
    }

    @Override
    public Class<GameFlagQuestProgression> getQuestType() {
        return GameFlagQuestProgression.class;
    }

    @Nonnull
    @Override
    public UUID getActorId() {
        return playerId;
    }
}
