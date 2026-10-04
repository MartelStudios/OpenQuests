package com.martelstudios.openquests.core.replication;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A count several servers move at once without ever overwriting each other: each keeps what it
 * added and what it took away in a slot of its own, and the count is what all slots come to. A
 * slot only ever grows, so merging two copies keeps the larger of each, in any order, any number
 * of times (a PN-counter).
 */
public final class ReplicatedCounter {

    private static final Codec<Map<String, Long>> SLOTS = new MapCodec<>(Codec.LONG, HashMap::new);

    public static final BuilderCodec<ReplicatedCounter> CODEC = BuilderCodec.builder(ReplicatedCounter.class, ReplicatedCounter::new)
                                                                             .append(new KeyedCodec<>("Added", SLOTS), (counter, slots) -> counter.added.putAll(slots), counter -> new HashMap<>(counter.added))
                                                                             .add()
                                                                             .append(new KeyedCodec<>("Removed", SLOTS), (counter, slots) -> counter.removed.putAll(slots), counter -> counter.removed.isEmpty() ? null : new HashMap<>(counter.removed))
                                                                             .add()
                                                                             .build();

    private final Map<String, Long> added = new ConcurrentHashMap<>();

    private final Map<String, Long> removed = new ConcurrentHashMap<>();

    /**
     * @return what every server's slots come to.
     */
    public long get() {
        long total = 0;
        for (long value : added.values()) total += value;
        for (long value : removed.values()) total -= value;
        return total;
    }

    /**
     * Moves the count by that much, in this server's slot.
     */
    public void add(long delta) {
        if (delta > 0) added.merge(Replica.localId(), delta, Long::sum);
        if (delta < 0) removed.merge(Replica.localId(), -delta, Long::sum);
    }

    /**
     * Brings the count to that value by moving this server's slot, which is all a server may write.
     */
    public void set(long value) {
        add(value - get());
    }

    /**
     * Reads a count written under a slot of its own, the same wherever it is read, so that merging
     * copies of it counts it once.
     */
    public void restore(@Nonnull String slot, long value) {
        if (value > 0) added.merge(slot, value, Math::max);
    }

    /**
     * Takes in what another copy knows: the larger of each slot.
     *
     * @return whether anything here changed.
     */
    public boolean merge(@Nonnull ReplicatedCounter other) {
        return mergeSlots(added, other.added) | mergeSlots(removed, other.removed);
    }

    private static boolean mergeSlots(@Nonnull Map<String, Long> mine, @Nonnull Map<String, Long> theirs) {
        boolean changed = false;
        for (Map.Entry<String, Long> slot : theirs.entrySet()) {
            Long before = mine.get(slot.getKey());
            if (before == null || before < slot.getValue()) {
                mine.merge(slot.getKey(), slot.getValue(), Math::max);
                changed = true;
            }
        }
        return changed;
    }
}
