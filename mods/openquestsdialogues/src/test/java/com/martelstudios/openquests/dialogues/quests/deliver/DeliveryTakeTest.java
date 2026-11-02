package com.martelstudios.openquests.dialogues.quests.deliver;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DeliveryTakeTest {

    private static Predicate<String> is(String id) {
        return id::equals;
    }

    @Test
    void takes_what_is_missing_and_no_more() {
        DeliveryTake.Taken taken = DeliveryTake.take(new String[]{"Fibre", "Stick", "Fibre"}, new int[]{3, 9, 4}, List.of(is("Fibre"), is("Stick")), new int[]{6, 2});

        assertArrayEquals(new int[]{3, 2, 3}, taken.removals());
        assertArrayEquals(new int[]{6, 2}, taken.perItem());
    }

    @Test
    void a_complete_item_takes_nothing() {
        DeliveryTake.Taken taken = DeliveryTake.take(new String[]{"Fibre"}, new int[]{5}, List.of(is("Fibre")), new int[]{0});

        assertArrayEquals(new int[]{0}, taken.removals());
        assertArrayEquals(new int[]{0}, taken.perItem());
    }

    @Test
    void a_slot_is_never_counted_twice() {
        DeliveryTake.Taken taken = DeliveryTake.take(new String[]{"Fibre", null}, new int[]{4, 0}, List.of(is("Fibre"), is("Fibre")), new int[]{3, 3});

        assertArrayEquals(new int[]{4, 0}, taken.removals());
        assertArrayEquals(new int[]{3, 1}, taken.perItem());
    }

    @Test
    void each_carried_item_gives_what_it_alone_would_take() {
        Predicate<String> anyPlant = id -> id.equals("Fibre") || id.equals("Leaf");
        Map<String, Integer> givable = DeliveryTake.givable(new String[]{"Leaf", "Fibre", null, "Oak", "Fibre"}, new int[]{5, 3, 0, 9, 4}, List.of(anyPlant), new int[]{6});

        assertEquals(List.of("Leaf", "Fibre"), List.copyOf(givable.keySet()));
        assertEquals(5, givable.get("Leaf"));
        assertEquals(6, givable.get("Fibre"));
    }

    @Test
    void only_what_left_the_inventory_is_credited() {
        // Planned 3 and 2, but the second slot gave up 1 only
        int[] removed = {3, 1};
        DeliveryTake.Taken credited = DeliveryTake.take(new String[]{"Fibre", "Stick"}, removed, List.of(is("Fibre"), is("Stick")), new int[]{3, 2});

        assertArrayEquals(new int[]{3, 1}, credited.perItem());
    }
}
