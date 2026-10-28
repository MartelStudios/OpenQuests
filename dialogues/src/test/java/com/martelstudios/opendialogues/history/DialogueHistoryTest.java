package com.martelstudios.opendialogues.history;

import com.martelstudios.opendialogues.CodecJson;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DialogueHistoryTest {

    @Test
    void historyCountsSurviveACodecRoundTrip() {
        DialogueHistory history = new DialogueHistory();
        history.recordOpened("Elder_Welcome");
        history.recordOpened("Elder_Welcome");
        history.recordAnswer("Elder_Welcome", "Accept");

        DialogueHistory read = CodecJson.roundTrip(DialogueHistory.CODEC, history);

        assertEquals(2, read.opened("Elder_Welcome"));
        assertEquals(1, read.answered("Elder_Welcome", "Accept"));
    }

    @Test
    void aCloneCountsOnItsOwn() {
        DialogueHistory history = new DialogueHistory();
        history.recordAnswer("Elder_Welcome", "Accept");

        DialogueHistory copy = (DialogueHistory) history.clone();
        copy.recordAnswer("Elder_Welcome", "Accept");
        copy.recordOpened("Elder_Welcome");

        assertEquals(1, history.answered("Elder_Welcome", "Accept"));
        assertEquals(0, history.opened("Elder_Welcome"));
        assertEquals(2, copy.answered("Elder_Welcome", "Accept"));
    }

    @Test
    void clearingOneDialogueKeepsTheOthers() {
        DialogueHistory history = new DialogueHistory();
        history.recordOpened("Elder_Welcome");
        history.recordAnswer("Elder_Welcome", "Accept");
        history.recordOpened("Smith_Hello");

        assertTrue(history.clear("Elder_Welcome"));
        assertEquals(0, history.opened("Elder_Welcome"));
        assertEquals(0, history.answered("Elder_Welcome", "Accept"));
        assertEquals(1, history.opened("Smith_Hello"));
    }

    @Test
    void clearingAnUnknownDialogueChangesNothing() {
        DialogueHistory history = new DialogueHistory();
        history.recordOpened("Smith_Hello");

        assertFalse(history.clear("Elder_Welcome"));
        assertEquals(1, history.opened("Smith_Hello"));
    }

    @Test
    void clearingEverythingEmptiesTheHistory() {
        DialogueHistory history = new DialogueHistory();
        history.recordOpened("Elder_Welcome");
        history.recordOpened("Smith_Hello");

        assertTrue(history.clear());
        assertEquals(0, history.opened("Elder_Welcome"));
        assertEquals(0, history.opened("Smith_Hello"));
        assertFalse(history.clear());
    }

    @Test
    void unknownDialogueCountsZero() {
        DialogueHistory history = new DialogueHistory();

        assertEquals(0, history.opened("Elder_Welcome"));
        assertEquals(0, history.answered("Elder_Welcome", "Accept"));
    }
}
