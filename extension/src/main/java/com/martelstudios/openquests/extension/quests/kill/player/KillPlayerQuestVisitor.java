package com.martelstudios.openquests.extension.quests.kill.player;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.martelstudios.openquests.core.visitors.QuestVisitor;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestProgression;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Counts one kill against the killer's quests that target this victim.
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
        if (quest.isOver()) return;
        if (!quest.matchesVictim(victim)) return;

        quest.apply(new QuantityQuestProgression.Add(1));

        LOGGER.at(Level.FINE).log("Quest %s progress for %s: %d/%d", quest.getId(), killerId, quest.getCurrentQuantity(), quest.getTargetQuantity());
    }

    @Override
    public Class<KillPlayerQuestProgression> getQuestType() {
        return KillPlayerQuestProgression.class;
    }

    @Nonnull
    @Override
    public UUID getActorId() {
        return killerId;
    }
}
