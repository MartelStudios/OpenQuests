package com.martelstudios.openquests.extension.quests.loot.npc;

import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestProgression;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LootRollTest {

    /**
     * Answers every chance roll with {@code chance} and every quantity roll with the top of its range.
     */
    private record Scripted(double chance) implements RandomGenerator {
        @Override
        public long nextLong() {
            throw new UnsupportedOperationException();
        }

        @Override
        public double nextDouble() {
            return chance;
        }

        @Override
        public int nextInt(int origin, int bound) {
            return bound - 1;
        }
    }

    private static LootRoll.Candidate candidate(int remaining, double chance, int min, int max) {
        return new LootRoll.Candidate(remaining, chance, min, max, 0, null);
    }

    @Test
    void highestChanceWins() {
        assertEquals(1, LootRoll.roll(List.of(candidate(5, 0.2, 1, 1), candidate(5, 0.5, 1, 1)), new Scripted(0.4)));
    }

    @Test
    void missAboveChance() {
        assertEquals(0, LootRoll.roll(List.of(candidate(5, 0.5, 1, 1)), new Scripted(0.6)));
    }

    @Test
    void reachedGuaranteeForces() {
        LootRoll.Candidate guaranteed = new LootRoll.Candidate(5, 0.01, 1, 1, 3, 4);

        assertEquals(1, LootRoll.roll(List.of(guaranteed), new Scripted(0.99)));
    }

    @Test
    void guaranteeNotYetReached() {
        LootRoll.Candidate waiting = new LootRoll.Candidate(5, 0.01, 1, 1, 2, 4);

        assertEquals(0, LootRoll.roll(List.of(waiting), new Scripted(0.99)));
    }

    @Test
    void quantityComesFromTheWinner() {
        assertEquals(3, LootRoll.roll(List.of(candidate(5, 0.2, 1, 1), candidate(5, 0.5, 2, 3)), new Scripted(0.1)));
    }

    @Test
    void theForcingCandidateSetsTheQuantity() {
        LootRoll.Candidate forcing = new LootRoll.Candidate(5, 0.2, 2, 3, 3, 4);

        assertEquals(3, LootRoll.roll(List.of(candidate(5, 0.5, 1, 1), forcing), new Scripted(0.99)));
    }

    @Test
    void quantityClampsToLargestRemaining() {
        assertEquals(2, LootRoll.roll(List.of(candidate(1, 0.5, 1, 3), candidate(2, 0.2, 1, 1)), new Scripted(0.1)));
    }

    @Test
    void singleNeedOfOneGetsOne() {
        assertEquals(1, LootRoll.roll(List.of(candidate(1, 1, 1, 3)), new Scripted(0.1)));
    }

    @Test
    void aFinishedStepNeverWinsTheRoll() {
        assertEquals(0, LootRoll.roll(List.of(candidate(0, 0.9, 1, 1), candidate(5, 0.1, 1, 1)), new Scripted(0.5)));
    }

    @Test
    void aFinishedStepNeverForcesTheRoll() {
        LootRoll.Candidate finished = new LootRoll.Candidate(0, 0.1, 1, 1, 9, 4);

        assertEquals(0, LootRoll.roll(List.of(finished, candidate(5, 0.1, 1, 1)), new Scripted(0.5)));
    }

    @Test
    void aHitResetsAfterCreditingAtMostTheNeed() {
        var operations = LootRoll.outcome(1, 2, 3, true);

        assertEquals(2, operations.size());
        assertEquals(2, ((QuantityQuestProgression.Add) operations.get(0)).amount());
        assertEquals(new LootNpcQuestProgression.ResetMisses(1), operations.get(1));
    }

    @Test
    void aLostDropIsAMiss() {
        assertEquals(List.of(new LootNpcQuestProgression.Miss(1)), LootRoll.outcome(1, 2, 3, false));
    }

    @Test
    void aMissIsAMiss() {
        assertEquals(List.of(new LootNpcQuestProgression.Miss(0)), LootRoll.outcome(0, 2, 0, false));
    }

    @Test
    void aFinishedStepTakesNothing() {
        assertEquals(List.of(), LootRoll.outcome(0, 0, 3, true));
        assertEquals(List.of(), LootRoll.outcome(0, 0, 0, false));
    }

    @Test
    void noCandidateDropsNothing() {
        assertEquals(0, LootRoll.roll(List.of(), new Scripted(0)));
    }
}
