package com.martelstudios.openquests.extension.quests.loot.npc;

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
            largestNeed = Math.max(largestNeed, candidate.remaining());
            if (winner != null && winner.forced()) continue;

            if (candidate.forced() || winner == null || candidate.chance() > winner.chance()) winner = candidate;
        }

        if (winner == null || largestNeed == 0) return 0;
        if (!winner.forced() && random.nextDouble() >= winner.chance()) return 0;

        return Math.min(random.nextInt(winner.min(), winner.max() + 1), largestNeed);
    }
}
