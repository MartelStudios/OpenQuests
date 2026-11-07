package com.martelstudios.opendialogues.characters.runtime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CharacterNameplateTest {

    @Test
    void firstNameFillsOnlyMissingComponents() {
        assertEquals(CharacterNameplate.Step.FillMissing, CharacterNameplate.decide(null, "key.name"));
    }

    @Test
    void changedFixedNameReplacesOurNameplate() {
        assertEquals(CharacterNameplate.Step.Replace, CharacterNameplate.decide("key.old", "key.new"));
    }

    @Test
    void sameNameKeepsNameplate() {
        assertEquals(CharacterNameplate.Step.Keep, CharacterNameplate.decide("key.name", "key.name"));
    }

    @Test
    void removedFixedNameTakesOurNameplateBack() {
        assertEquals(CharacterNameplate.Step.Remove, CharacterNameplate.decide("key.old", null));
    }

    @Test
    void nothingOursAndNoNameKeepsWhatIsThere() {
        assertEquals(CharacterNameplate.Step.Keep, CharacterNameplate.decide(null, null));
    }
}
