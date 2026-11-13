package com.martelstudios.opendialogues.characters.runtime;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * The NPCs waiting for a taken claim slot, in order of arrival. Nothing is saved: NPCs enlist
 * again as their chunks load.
 *
 * @param <W> how a waiting NPC is known.
 */
public final class CharacterWaitlist<W> {

    private final Map<ClaimSlot, SequencedSet<W>> queues = new HashMap<>();

    /**
     * Makes the NPC wait for exactly these slots, keeping its place in those it already waited for.
     */
    public synchronized void waitFor(@Nonnull W waiter, @Nonnull List<ClaimSlot> slots) {
        queues.entrySet().removeIf(entry -> !slots.contains(entry.getKey()) && entry.getValue().remove(waiter) && entry.getValue().isEmpty());
        for (ClaimSlot slot : slots) queues.computeIfAbsent(slot, key -> new LinkedHashSet<>()).add(waiter);
    }

    /**
     * Stops the NPC waiting for anything.
     */
    public synchronized void forget(@Nonnull W waiter) {
        waitFor(waiter, List.of());
    }

    /**
     * Stops the matching NPCs waiting, for a world closing: its store drops its NPCs without removing them.
     */
    public synchronized void forgetIf(@Nonnull Predicate<W> matches) {
        queues.values().forEach(queue -> queue.removeIf(matches));
        queues.values().removeIf(Set::isEmpty);
    }

    /**
     * @return the NPC waiting longest for the slot, if any.
     */
    @Nonnull
    public synchronized Optional<W> first(@Nonnull ClaimSlot slot) {
        SequencedSet<W> queue = queues.get(slot);
        return queue == null ? Optional.empty() : Optional.of(queue.getFirst());
    }
}
