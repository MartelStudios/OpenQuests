package com.martelstudios.openquests.extension.quests.loot.npc;

import com.martelstudios.openquests.core.models.QuestOperation;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestProgression;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.random.RandomGenerator;

/**
 * The one roll a death makes for an item, however many running steps want it: holding two quests
 * never rolls twice, nor lowers the odds.
 */
public final class LootRoll {

    private LootRoll() {}

    /**
     * One running step wanting the item, with the source that applies to the victim.
     *
     * @param remaining       items the step still needs
     * @param misses          kills in a row without a drop for that source
     * @param guaranteedAfter the kill that is forced to drop, {@code null} for none
     */
    public record Candidate(int remaining, double chance, int min, int max, int misses, @Nullable Integer guaranteedAfter) {

        boolean forced() {
            return guaranteedAfter != null && misses >= guaranteedAfter - 1;
        }
    }

    /**
     * A reached guarantee forces the drop, else the highest chance rolls once. The winner's range
     * sets the quantity, capped at the largest need so nobody gets a surplus to hoard.
     *
     * @return the quantity dropped, 0 for nothing.
     */
    public static int roll(@Nonnull List<Candidate> candidates, @Nonnull RandomGenerator random) {
        Candidate winner = null;
        int largestNeed = 0;

        for (Candidate candidate : candidates) {
            // A step kept running past its target needs nothing, so it neither forces nor sets the odds
            if (candidate.remaining() <= 0) continue;

            largestNeed = Math.max(largestNeed, candidate.remaining());
            if (winner != null && winner.forced()) continue;

            if (candidate.forced() || winner == null || candidate.chance() > winner.chance()) winner = candidate;
        }

        if (winner == null || largestNeed == 0) return 0;
        if (!winner.forced() && random.nextDouble() >= winner.chance()) return 0;

        return Math.min(random.nextInt(winner.min(), winner.max() + 1), largestNeed);
    }

    /**
     * What the roll means for one step: a drop that reached the player is credited up to the need,
     * then the run of misses starts over; anything else, a lost drop included, is a miss so the
     * guarantee still comes. A step needing nothing takes nothing, or it would be written on every kill.
     *
     * @param remaining what the step needed before the roll
     * @param received  whether the items reached the player's inventory
     */
    @Nonnull
    public static List<QuestOperation<? super LootNpcQuestProgression>> outcome(int entry, int remaining, int quantity, boolean received) {
        if (remaining <= 0) return List.of();
        if (!received || quantity <= 0) return List.of(new LootNpcQuestProgression.Miss(entry));

        return List.of(new QuantityQuestProgression.Add(Math.min(quantity, remaining)), new LootNpcQuestProgression.ResetMisses(entry));
    }
}
