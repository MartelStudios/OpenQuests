package com.martelstudios.openquests.dialogues.quests.deliver;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Predicate;

/**
 * Works out what a delivery takes from an inventory, kept apart from the inventory itself so the
 * counting can be checked without a server.
 */
public final class DeliveryTake {

    private DeliveryTake() {}

    /**
     * @param removals how many to take from each slot.
     * @param perItem  how many each requested item receives.
     */
    public record Taken(@Nonnull int[] removals, @Nonnull int[] perItem) {}

    /**
     * Takes for each item in order, slot by slot, at most what it still misses, so two items
     * accepting the same stack never count it twice.
     *
     * @param slotIds the item in each slot, {@code null} when empty.
     */
    @Nonnull
    public static Taken take(@Nonnull String[] slotIds, @Nonnull int[] slotQuantities, @Nonnull List<Predicate<String>> items, @Nonnull int[] missing) {
        int[] left = slotQuantities.clone();
        int[] removals = new int[slotIds.length];
        int[] perItem = new int[items.size()];
        for (int item = 0; item < items.size(); item++) {
            int needed = missing[item];
            for (int slot = 0; slot < slotIds.length && needed > 0; slot++) {
                if (left[slot] <= 0 || slotIds[slot] == null || !items.get(item).test(slotIds[slot])) continue;
                int taken = Math.min(left[slot], needed);
                left[slot] -= taken;
                removals[slot] += taken;
                perItem[item] += taken;
                needed -= taken;
            }
        }
        return new Taken(removals, perItem);
    }
}
