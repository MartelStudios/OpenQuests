package com.martelstudios.opendialogues.characters.runtime;

import com.martelstudios.opendialogues.characters.CharacterFixtures;
import com.martelstudios.opendialogues.characters.OpenDialogueCharacterAsset;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CharacterWaitlistTest {

    private static final UUID X = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID Y = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final Map<String, OpenDialogueCharacterAsset> CHARACTERS = Map.of(
            "Oak", CharacterFixtures.character("Oak", "{ \"Uniqueness\": \"Alive\" }"),
            "Mira", CharacterFixtures.character("Mira", "{ \"Uniqueness\": \"Forever\", \"Scope\": \"Universe\" }"),
            "Villager", CharacterFixtures.character("Villager", "{}"));

    private final ClaimSlot s = new ClaimSlot("S", null);
    private final ClaimSlot t = new ClaimSlot("T", null);
    private final CharacterWaitlist<String> waitlist = new CharacterWaitlist<>();

    @Test
    void firstIsTheEarliestWaiter() {
        waitlist.waitFor("a", List.of(s));
        waitlist.waitFor("b", List.of(s));

        assertEquals(Optional.of("a"), waitlist.first(s));
    }

    @Test
    void waitForKeepsAnExistingPlace() {
        waitlist.waitFor("a", List.of(s));
        waitlist.waitFor("b", List.of(s, t));
        waitlist.waitFor("a", List.of(s, t));

        assertEquals(Optional.of("a"), waitlist.first(s));
        assertEquals(Optional.of("b"), waitlist.first(t));
    }

    @Test
    void waitForLeavesSlotsNoLongerNamed() {
        waitlist.waitFor("a", List.of(s, t));
        waitlist.waitFor("a", List.of(t));

        assertEquals(Optional.empty(), waitlist.first(s));
        assertEquals(Optional.of("a"), waitlist.first(t));
    }

    @Test
    void forgetLeavesEveryQueue() {
        waitlist.waitFor("a", List.of(s, t));
        waitlist.forget("a");

        assertEquals(Optional.empty(), waitlist.first(s));
        assertEquals(Optional.empty(), waitlist.first(t));
    }

    @Test
    void worldSlotsOfTwoWorldsStayApart() {
        assertNotEquals(ClaimSlot.of("Oak", CHARACTERS::get, X), ClaimSlot.of("Oak", CHARACTERS::get, Y));
        assertEquals(new ClaimSlot("Oak", X), ClaimSlot.of("Oak", CHARACTERS::get, X));
    }

    @Test
    void universeSlotIgnoresTheWorld() {
        assertEquals(new ClaimSlot("Mira", null), ClaimSlot.of("Mira", CHARACTERS::get, X));
        assertEquals(ClaimSlot.of("Mira", CHARACTERS::get, X), ClaimSlot.of("Mira", CHARACTERS::get, Y));
    }

    @Test
    void slotOfARepeatableOrUnknownCharacterIsNull() {
        assertNull(ClaimSlot.of("Villager", CHARACTERS::get, X));
        assertNull(ClaimSlot.of("Ghost", CHARACTERS::get, X));
    }
}
