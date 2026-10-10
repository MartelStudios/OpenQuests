package com.martelstudios.openquests.extension.quests.movement;

import com.hypixel.hytale.protocol.MovementStates;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.visitors.QuestVisitor;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Carries one tick of movement to every quest counting it. Typed on the base rather than on each
 * concrete type so that walking, running and jumping are answered in a single pass over the
 * player's quests: the sample is the same for all three, only what they make of it differs.
 */
public class MovementQuestVisitor implements QuestVisitor<MovementQuestProgression<?>> {

    private final UUID playerId;
    private final MovementStates states;
    private final double metres;
    private final double seconds;

    /**
     * The whole units each quest made of this sample, worked out once on the copy running here:
     * what is left of a unit lives on that copy alone, and a stored copy this runs again on counts
     * the same.
     */
    private final Map<UUID, Integer> counted = new ConcurrentHashMap<>();

    public MovementQuestVisitor(@Nonnull UUID playerId, @Nonnull MovementStates states, double metres, double seconds) {
        this.playerId = playerId;
        this.states = states;
        this.metres = metres;
        this.seconds = seconds;
    }

    @Override
    public void progress(MovementQuestProgression<?> quest) {
        if (!quest.getPlayers().contains(playerId)) return;
        if (quest.isOver()) return;

        // Every sample is offered, even an empty one: a jump is counted from the tick it starts on,
        // which a quest can only tell apart by having been shown the tick before
        int whole = counted.computeIfAbsent(quest.getId(), id -> quest.accumulate(states, metres, seconds));
        if (whole <= 0) return;

        quest.addQuantity(whole);
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
