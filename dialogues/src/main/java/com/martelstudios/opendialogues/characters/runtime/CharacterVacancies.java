package com.martelstudios.opendialogues.characters.runtime;

import javax.annotation.Nonnull;
import java.util.Optional;

/**
 * Hands a freed claim slot to the NPC waiting longest for it, skipping those that no longer can
 * take it. A waiter that fails leaves the queue, so a release costs one redraw on average.
 *
 * @param <W> how a waiting NPC is known.
 */
public final class CharacterVacancies<W> {

    private final CharacterWaitlist<W> waitlist;
    private final VacancyPort<W> port;

    /**
     * @param port reaches the waiters' worlds, so this order is tested without a server.
     */
    public CharacterVacancies(@Nonnull CharacterWaitlist<W> waitlist, @Nonnull VacancyPort<W> port) {
        this.waitlist = waitlist;
        this.port = port;
    }

    /**
     * Serves the slot from its queue, each waiter on its own world thread.
     */
    public void fill(@Nonnull ClaimSlot slot) {
        Optional<W> first = waitlist.first(slot);
        if (first.isEmpty()) return;
        W waiter = first.get();
        if (!port.execute(waiter, () -> serve(slot, waiter))) {
            waitlist.forget(waiter);
            fill(slot);
        }
    }

    private void serve(@Nonnull ClaimSlot slot, @Nonnull W waiter) {
        if (port.isGone(waiter)) {
            waitlist.forget(waiter);
            fill(slot);
            return;
        }
        // A slot taken again, or consumed for good, leaves the queue as it is.
        if (!port.isAvailable(waiter, slot.characterId())) return;

        VacancyPort.Redraw redraw = port.redraw(waiter);
        if (redraw.served()) waitlist.forget(waiter);
        else waitlist.waitFor(waiter, redraw.blocked());
        if (waitlist.first(slot).equals(Optional.of(waiter))) return;
        if (port.isAvailable(waiter, slot.characterId())) fill(slot);
    }
}
