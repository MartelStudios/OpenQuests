package com.martelstudios.hyquests.extension.quests.kill;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.martelstudios.hyquests.core.models.QuestState;
import com.martelstudios.hyquests.core.visitors.QuestVisitor;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Counts one kill against the killer's quests, for the quests that were after this victim.
 */
public class KillPlayerQuestVisitor implements QuestVisitor<KillPlayerQuestProgression> {
    @Nonnull
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final UUID killerId;
    private final PlayerRef victim;

    public KillPlayerQuestVisitor(@Nonnull UUID killerId, @Nonnull PlayerRef victim) {
        this.killerId = killerId;
        this.victim = victim;
    }

    @Override
    public void progress(KillPlayerQuestProgression quest) {
        if (!quest.getPlayers().contains(killerId)) return;
        if (quest.isCompleted()) return;
        if (!quest.matchesVictim(victim)) return;

        quest.setCount(quest.getCount() + 1)
             .setState(quest.checkCompletion() ? QuestState.SUCCESSFUL : QuestState.IN_PROGRESS)
             .markDirty();

        LOGGER.at(Level.FINE).log("Quest %s progress for %s: %d/%d", quest.getId(), killerId, quest.getCount(), quest.getTargetCount());
    }

    @Override
    public Class<KillPlayerQuestProgression> getQuestType() {
        return KillPlayerQuestProgression.class;
    }
}
