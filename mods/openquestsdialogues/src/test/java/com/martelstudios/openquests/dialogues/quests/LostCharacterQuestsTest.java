package com.martelstudios.openquests.dialogues.quests;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LostCharacterQuestsTest {

    private static final Predicate<String> HERO_LOST = Set.of("Hero")::contains;

    @Test
    void running_quest_aimed_at_a_lost_character_fails() {
        assertTrue(LostCharacterQuests.shouldFail("Hero", false, HERO_LOST));
    }

    @Test
    void quest_already_over_is_left_alone() {
        assertFalse(LostCharacterQuests.shouldFail("Hero", true, HERO_LOST));
    }

    @Test
    void quest_without_target_or_aimed_elsewhere_is_left_alone() {
        assertFalse(LostCharacterQuests.shouldFail(null, false, HERO_LOST));
        assertFalse(LostCharacterQuests.shouldFail("Farmer", false, HERO_LOST));
    }

    @Test
    void quest_loaded_before_reaching_its_holder_waits_for_the_history_catch_up() {
        assertFalse(LostCharacterQuests.failsOnLoad("Hero", false, HERO_LOST, false));
        assertTrue(LostCharacterQuests.failsOnLoad("Hero", false, HERO_LOST, true));
    }

    @Test
    void other_quest_types_have_no_target() {
        assertNull(LostCharacterQuests.targetOf(null));
    }
}
