package com.martelstudios.openquests.extension.quests.movement;

import com.hypixel.hytale.protocol.MovementStates;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.visitors.QuestVisitor;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Carries one tick of movement to every quest counting it. Typed on the base rather than on each
 * concrete type so that walking, running and jumping are answered in a single pass over the
 * player's quests: the sample is the same for all three, only what they make of it differs.
 */
public class MovementQuestVisitor implements QuestVisitor<MovementQuestProgression<?>> {

    private final UUID playerId;
    private final MovementStates states;
    private final double metres;

    public MovementQuestVisitor(@Nonnull UUID playerId, @Nonnull MovementStates states, double metres) {
        this.playerId = playerId;
        this.states = states;
        this.metres = metres;
    }

    @Override
    public void progress(MovementQuestProgression<?> quest) {
        if (!quest.getPlayers().contains(playerId)) return;
        if (quest.isCompleted() && quest.isStopOnComplete()) return;

        // Every sample is offered, even an empty one: a jump is counted from the tick it starts on,
        // which a quest can only tell apart by having been shown the tick before
        if (!quest.accumulate(states, metres)) return;

        if (quest.checkCompletion()) quest.setState(QuestState.SUCCESSFUL);

        quest.markDirty();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<MovementQuestProgression<?>> getQuestType() {
        return (Class<MovementQuestProgression<?>>) (Class<?>) MovementQuestProgression.class;
    }

    @Nonnull
    @Override
    public UUID getActorId() {
        return playerId;
    }
}
