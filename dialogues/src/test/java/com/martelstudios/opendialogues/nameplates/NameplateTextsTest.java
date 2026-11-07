package com.martelstudios.opendialogues.nameplates;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NameplateTextsTest {

    @Test
    void usesViewerLanguage() {
        assertEquals("Brindille", NameplateTexts.text((language, key) -> language.equals("fr-FR") ? "Brindille" : "Twig", "fr-FR", "en-US", "k"));
    }

    @Test
    void fallsBackToDefaultThenKey() {
        assertEquals("Twig", NameplateTexts.text((language, key) -> language.equals("en-US") ? "Twig" : null, "fr-FR", "en-US", "k"));
        assertEquals("k", NameplateTexts.text((language, key) -> null, "fr-FR", "en-US", "k"));
    }

    @Test
    void recognisesOurPlate() {
        assertTrue(NameplateTexts.isOurs("Twig", "k", "Twig"));
        assertTrue(NameplateTexts.isOurs("k", "k", null));
    }

    @Test
    void keepsHandSetPlate() {
        assertFalse(NameplateTexts.isOurs("Village elder", "k", "Twig"));
    }
}
