package com.martelstudios.openquests.extension.rewards.random;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.martelstudios.openquests.core.rewards.QuestReward;
import com.martelstudios.openquests.core.utils.EntityComponents;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Grants one of its rewards, drawn by weight. Weights are relative, so they need not add up to
 * anything: 3 and 1 is three chances in four.
 */
public class RandomReward extends QuestReward {

    public static final BuilderCodec<RandomReward> CODEC = BuilderCodec.builder(RandomReward.class, RandomReward::new, QuestReward.BASE_CODEC)
                                                                       .append(new KeyedCodec<>("Rewards", new ArrayCodec<>(Entry.CODEC, Entry[]::new), true), (reward, entries) -> reward.entries = entries, reward -> reward.entries)
                                                                       .addValidator(Validators.nonEmptyArray())
                                                                       .add()
                                                                       .build();

    protected Entry[] entries = new Entry[0];

    private RandomReward() {}

    /**
     * Draws again on every attempt: a reward that failed granted nothing, so nothing was won yet.
     */
    @Override
    public boolean grant(@Nonnull UUID sourceQuestId, @Nonnull EntityComponents playerComponents) {
        return draw().reward.grant(sourceQuestId, playerComponents);
    }

    /**
     * @return the rewards it draws from, in the order written.
     */
    @Nonnull
    public Entry[] getEntries() {
        return entries;
    }

    /**
     * @return the sum of every weight, what each entry's chance is out of.
     */
    public double getTotalWeight() {
        double total = 0;
        for (Entry entry : entries) total += entry.weight;
        return total;
    }

    @Nonnull
    private Entry draw() {
        double roll = ThreadLocalRandom.current().nextDouble(getTotalWeight());
        for (Entry entry : entries) {
            roll -= entry.weight;
            if (roll < 0) return entry;
        }
        // Only reached through rounding on the last entry
        return entries[entries.length - 1];
    }

    /**
     * One reward of the draw and its weight, 1 when left out.
     */
    public static class Entry {

        public static final BuilderCodec<Entry> CODEC = BuilderCodec.builder(Entry.class, Entry::new)
                                                                    .append(new KeyedCodec<>("Reward", QuestReward.CODEC, true), (entry, reward) -> entry.reward = reward, entry -> entry.reward)
                                                                    .addValidator(Validators.nonNull())
                                                                    .add()
                                                                    .append(new KeyedCodec<>("Weight", Codec.DOUBLE), (entry, weight) -> entry.weight = weight, entry -> Double.valueOf(entry.weight))
                                                                    .addValidator(Validators.greaterThan(0.0))
                                                                    .add()
                                                                    .build();

        protected QuestReward reward;

        protected double weight = 1;

        private Entry() {}

        /**
         * @return the reward granted when this entry is drawn.
         */
        @Nonnull
        public QuestReward getReward() {
            return reward;
        }

        /**
         * @return its weight, relative to the others of the draw.
         */
        public double getWeight() {
            return weight;
        }
    }
}
