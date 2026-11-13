package com.martelstudios.openquests.extension.quests.reachlocation;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.server.core.Message;
import com.martelstudios.openquests.core.models.QuestOperation;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestProgression;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Counts the spots of a path reached. On an ordered path the count is the index of the next spot;
 * otherwise the spots reached are written down.
 */
public class ReachPathQuestProgression extends QuantityQuestProgression<ReachPathQuestProgression> {

    public static final BuilderCodec<ReachPathQuestProgression> CODEC =
        BuilderCodec.builder(ReachPathQuestProgression.class, ReachPathQuestProgression::new, QuantityQuestProgression.BASE_CODEC)
                    .append(new KeyedCodec<>("Reached", new ArrayCodec<>(Codec.INTEGER, Integer[]::new)), (quest, reached) -> quest.reached.addAll(java.util.List.of(reached)), quest -> quest.reached.toArray(Integer[]::new))
                    .add()
                    .build();

    private Set<Integer> reached = ConcurrentHashMap.newKeySet();

    @Override
    protected void restartCombo() {
        super.restartCombo();
        reached.clear();
    }

    protected ReachPathQuestProgression() {}

    @Override
    public ReachPathQuestAsset getAsset() {
        return (ReachPathQuestAsset) super.getAsset();
    }

    /**
     * @return whether that spot is one the player is to reach now.
     */
    public boolean isAwaited(int spot) {
        return isAwaitedAt(spot, now());
    }

    /**
     * @return whether that spot is one the player is to reach at that moment: a combo window closed
     * by then gives every spot back.
     */
    public boolean isAwaitedAt(int spot, @Nonnull Instant at) {
        int count = getCurrentQuantityAt(at);
        if (getAsset().isOrdered()) return spot == count;
        return spot >= 0 && (count == 0 || !reached.contains(spot));
    }

    @Nonnull
    @Override
    public Message getDefaultTitle() {
        return Message.translation("openquests.quest.default.reach-path").param("quantity", getTargetQuantity());
    }

    /**
     * Counts a spot reached, one awaited or nothing happens.
     */
    public record Reach(int spot, @Nonnull Instant at) implements QuestOperation<ReachPathQuestProgression> {

        /**
         * A spot reached now.
         */
        public Reach(int spot) {
            this(spot, Instant.now());
        }

        @Override
        public boolean applyTo(@Nonnull ReachPathQuestProgression quest) {
            if (quest.isOver() || !quest.isAwaitedAt(spot, at)) return false;

            // Counted first: a combo restarting forgets the spots reached, this one aside
            boolean counted = new Add(1, at).applyTo(quest);
            if (counted && !quest.getAsset().isOrdered()) quest.reached.add(spot);
            return counted;
        }
    }

    /**
     * Sends the path back to its first spot.
     */
    public record Restart() implements QuestOperation<ReachPathQuestProgression> {
        @Override
        public boolean applyTo(@Nonnull ReachPathQuestProgression quest) {
            quest.reached.clear();
            return quest.moveCountTo(0);
        }
    }
}
