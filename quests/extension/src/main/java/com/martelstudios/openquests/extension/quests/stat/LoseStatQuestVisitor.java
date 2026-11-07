package com.martelstudios.openquests.extension.quests.stat;

import com.martelstudios.openquests.core.visitors.QuestVisitor;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestProgression;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.function.ToDoubleFunction;

/**
 * Carries one tick of a player's stat losses to every quest counting one of them. Each quest reads
 * its own stat, so two quests on the same stat both count the whole loss.
 */
public class LoseStatQuestVisitor implements QuestVisitor<LoseStatQuestProgression> {

    private final UUID playerId;
    private final ToDoubleFunction<String> lossOfStat;

    /**
     * @param lossOfStat the points the player lost this tick, by stat id.
     */
    public LoseStatQuestVisitor(@Nonnull UUID playerId, @Nonnull ToDoubleFunction<String> lossOfStat) {
        this.playerId = playerId;
        this.lossOfStat = lossOfStat;
    }

    @Override
    public void progress(LoseStatQuestProgression quest) {
        if (!quest.getPlayers().contains(playerId)) return;
        if (quest.isOver()) return;

        int points = quest.accumulate(lossOfStat.applyAsDouble(quest.getStat()));
        if (points > 0) quest.apply(new QuantityQuestProgression.Add(points));
    }

    @Override
    public Class<LoseStatQuestProgression> getQuestType() {
        return LoseStatQuestProgression.class;
    }

    @Nonnull
    @Override
    public UUID getActorId() {
        return playerId;
    }
}
