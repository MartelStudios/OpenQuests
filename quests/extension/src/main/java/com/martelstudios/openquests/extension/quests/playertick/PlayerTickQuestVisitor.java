package com.martelstudios.openquests.extension.quests.playertick;

import com.hypixel.hytale.protocol.MovementStates;
import com.martelstudios.openquests.core.visitors.QuestVisitor;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestProgression;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Carries the same player tick sample to time and movement quests in a single pass.
 */
public class PlayerTickQuestVisitor implements QuestVisitor<PlayerTickQuestProgression<?>> {

    private final UUID playerId;
    private final MovementStates states;
    private final double metres;
    private final double seconds;

    public PlayerTickQuestVisitor(@Nonnull UUID playerId, @Nonnull MovementStates states, double metres, double seconds) {
        this.playerId = playerId;
        this.states = states;
        this.metres = metres;
        this.seconds = seconds;
    }

    @Override
    public void progress(PlayerTickQuestProgression<?> quest) {
        if (!quest.getPlayers().contains(playerId)) return;
        if (quest.isOver()) return;

        // Every sample is offered, even an empty one: a jump is counted from the tick it starts on,
        // which a quest can only tell apart by having been shown the tick before
        quest.apply(new QuantityQuestProgression.Add(quest.accumulate(states, metres, seconds)));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<PlayerTickQuestProgression<?>> getQuestType() {
        return (Class<PlayerTickQuestProgression<?>>) (Class<?>) PlayerTickQuestProgression.class;
    }

    @Nonnull
    @Override
    public UUID getActorId() {
        return playerId;
    }
}
