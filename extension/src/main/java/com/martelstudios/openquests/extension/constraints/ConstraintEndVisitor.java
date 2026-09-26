package com.martelstudios.openquests.extension.constraints;

import com.martelstudios.openquests.core.constraints.QuestConstraint;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.visitors.QuestVisitor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;
import java.util.function.Function;

/**
 * Ends the running quests of one player whose asset carries a constraint objecting to what just
 * happened to them. Something that happened to the player rather than something they did, so it
 * carries no actor and no constraint holds it back.
 */
public class ConstraintEndVisitor implements QuestVisitor<AbstractQuestProgression<?>> {

    private final UUID playerId;

    /**
     * The outcome a constraint ends the quest on, {@code null} for one with no objection.
     */
    private final Function<QuestConstraint, QuestState> outcomeOf;

    public ConstraintEndVisitor(@Nonnull UUID playerId, @Nonnull Function<QuestConstraint, QuestState> outcomeOf) {
        this.playerId = playerId;
        this.outcomeOf = outcomeOf;
    }

    /**
     * The first constraint to object decides the outcome: a quest ends once.
     */
    @Override
    public void progress(AbstractQuestProgression<?> quest) {
        if (!quest.getPlayers().contains(playerId)) return;
        if (quest.isCompleted()) return;

        OpenQuestAsset asset = quest.getAsset();
        if (asset == null) return;

        for (QuestConstraint constraint : asset.getConstraints()) {
            QuestState outcome = outcomeOf.apply(constraint);
            if (outcome == null) continue;

            quest.setState(outcome).markDirty();
            return;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<AbstractQuestProgression<?>> getQuestType() {
        return (Class<AbstractQuestProgression<?>>) (Class<?>) AbstractQuestProgression.class;
    }

    /**
     * @return {@code null} when that constraint has nothing to say, for a caller writing the
     * function as one expression.
     */
    @Nullable
    public static QuestState failIf(boolean objects) {
        return objects ? QuestState.FAILED : null;
    }
}
