package com.martelstudios.openquests.extension.quests.reachvalue;

import com.martelstudios.openquests.core.visitors.QuestVisitor;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.function.ToIntFunction;

/**
 * Counts the value each of the player's quests watches. A value lowered afterwards never reopens
 * a quest already over.
 */
public class ReachValueQuestVisitor implements QuestVisitor<ReachValueQuestProgression> {

    private final UUID playerId;
    private final ToIntFunction<ValueSource> values;

    /**
     * @param values reads a source for this player.
     */
    public ReachValueQuestVisitor(@Nonnull UUID playerId, @Nonnull ToIntFunction<ValueSource> values) {
        this.playerId = playerId;
        this.values = values;
    }

    @Override
    public void progress(ReachValueQuestProgression quest) {
        if (!quest.getPlayers().contains(playerId)) return;
        if (quest.isOver()) return;

        ReachValueQuestAsset asset = quest.getAsset();
        quest.apply(new ReachValueQuestProgression.Read(values.applyAsInt(asset.getSource()), asset.isRelative()));
    }

    @Override
    public Class<ReachValueQuestProgression> getQuestType() {
        return ReachValueQuestProgression.class;
    }

    @Nonnull
    @Override
    public UUID getActorId() {
        return playerId;
    }
}
