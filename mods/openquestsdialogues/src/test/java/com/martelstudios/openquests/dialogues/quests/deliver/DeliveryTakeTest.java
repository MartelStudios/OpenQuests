package com.martelstudios.openquests.dialogues.quests.deliver;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

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
}
