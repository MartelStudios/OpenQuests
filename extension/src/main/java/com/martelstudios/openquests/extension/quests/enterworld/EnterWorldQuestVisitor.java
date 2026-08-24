package com.martelstudios.openquests.extension.quests.enterworld;

import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.visitors.QuestVisitor;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Completes the entering player's quests whose pattern matches the world they just entered.
 */
public class EnterWorldQuestVisitor implements QuestVisitor<EnterWorldQuestProgression> {

    private final UUID playerId;
    private final String worldName;

    public EnterWorldQuestVisitor(@Nonnull UUID playerId, @Nonnull String worldName) {
        this.playerId = playerId;
        this.worldName = worldName;
    }

    @Override
    public void progress(EnterWorldQuestProgression quest) {
        if (!quest.getPlayers().contains(playerId)) return;
        if (quest.isCompleted() && quest.isStopOnComplete()) return;
        if (!quest.matchesWorld(worldName)) return;

        quest.setState(QuestState.SUCCESSFUL).markDirty();
    }

    @Override
    public Class<EnterWorldQuestProgression> getQuestType() {
        return EnterWorldQuestProgression.class;
    }
}
