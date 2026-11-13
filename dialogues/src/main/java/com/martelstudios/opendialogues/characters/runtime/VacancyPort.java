package com.martelstudios.opendialogues.characters.runtime;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * What serving a freed slot needs from the game, so the serving order is tested without a server.
 *
 * @param <W> how a waiting NPC is known.
 */
public interface VacancyPort<W> {

    /**
     * What a waiter's redraw gave.
     *
     * @param served  whether the NPC now carries a character.
     * @param blocked the slots it waits for after the redraw.
     */
    record Redraw(boolean served, @Nonnull List<ClaimSlot> blocked) {}

    /**
     * Runs the task on the waiter's world thread.
     *
     * @return false when that world is gone, the task then never runs.
     */
    boolean execute(@Nonnull W waiter, @Nonnull Runnable task);

    /**
     * @return whether the NPC left its world or already carries a character, on its world thread.
     */
    boolean isGone(@Nonnull W waiter);

    /**
     * @return whether the NPC could be given the character now, on its world thread.
     */
    boolean isAvailable(@Nonnull W waiter, @Nonnull String characterId);

    /**
     * Lets the NPC draw again, on its world thread.
     */
    @Nonnull
    Redraw redraw(@Nonnull W waiter);
}
