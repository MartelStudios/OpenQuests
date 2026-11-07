package com.martelstudios.opendialogues.nameplates;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TranslatedNameplateTest {

    @Test
    void sendsOnFirstSightThenNotAgain() {
        TranslatedNameplate plate = new TranslatedNameplate("k");
        Object viewer = new Object();
        assertTrue(plate.needsSend(viewer, "fr-FR", true));
        assertFalse(plate.needsSend(viewer, "fr-FR", false));
    }

    @Test
    void resendsOnlyToViewerWhoChangedLanguage() {
        TranslatedNameplate plate = new TranslatedNameplate("k");
        Object a = new Object();
        Object b = new Object();
        plate.needsSend(a, "fr-FR", true);
        plate.needsSend(b, "en-US", true);
        assertTrue(plate.needsSend(a, "en-US", false));
        assertFalse(plate.needsSend(b, "en-US", false));
    }

    @Test
    void resendsWhenViewerComesBack() {
        TranslatedNameplate plate = new TranslatedNameplate("k");
        Object viewer = new Object();
        plate.needsSend(viewer, "fr-FR", true);
        plate.retainViewers(Set.of());
        assertTrue(plate.needsSend(viewer, "fr-FR", false));
    }

    @Test
    void newKeyResendsToEveryone() {
        TranslatedNameplate plate = new TranslatedNameplate("k");
        Object viewer = new Object();
        plate.needsSend(viewer, "fr-FR", true);
        plate.setKey("other");
        assertEquals("other", plate.getKey());
        assertTrue(plate.needsSend(viewer, "fr-FR", false));
    }

    @Test
    void sameKeyKeepsMemory() {
        TranslatedNameplate plate = new TranslatedNameplate("k");
        Object viewer = new Object();
        plate.needsSend(viewer, "fr-FR", true);
        plate.setKey("k");
        assertFalse(plate.needsSend(viewer, "fr-FR", false));
    }

    @Test
    void cloneForgetsViewers() {
        TranslatedNameplate plate = new TranslatedNameplate("k");
        Object viewer = new Object();
        plate.needsSend(viewer, "fr-FR", true);
        TranslatedNameplate copy = (TranslatedNameplate) plate.clone();
        assertEquals("k", copy.getKey());
        assertTrue(copy.needsSend(viewer, "fr-FR", false));
    }
}
