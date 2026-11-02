package com.martelstudios.openquests.dialogues.quests.deliver;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeliveryTakeOverTest {

    @Test
    void the_first_matching_quest_wins() {
        assertEquals(0, DeliveryTakeOver.pick(List.of("Farmer", "Farmer"), List.of(new int[]{5}, new int[]{5}), List.of(new int[]{1}, new int[]{1}), "Farmer"));
    }

    @Test
    void a_quest_with_nothing_missing_is_skipped() {
        assertEquals(1, DeliveryTakeOver.pick(List.of("Farmer", "Farmer"), List.of(new int[]{0}, new int[]{3}), List.of(new int[]{4}, new int[]{4}), "Farmer"));
    }

    @Test
    void a_quest_whose_items_are_not_carried_is_skipped() {
        assertEquals(1, DeliveryTakeOver.pick(List.of("Farmer", "Farmer"), List.of(new int[]{5, 2}, new int[]{5}), List.of(new int[]{0, 0}, new int[]{2}), "Farmer"));
    }

    @Test
    void another_character_quest_is_skipped() {
        assertEquals(-1, DeliveryTakeOver.pick(List.of("Mage"), List.of(new int[]{5}), List.of(new int[]{5}), "Farmer"));
    }

    @Test
    void a_carried_item_that_is_complete_does_not_count() {
        assertEquals(-1, DeliveryTakeOver.pick(List.of("Farmer"), List.of(new int[]{0, 3}), List.of(new int[]{6, 0}), "Farmer"));
    }
}
