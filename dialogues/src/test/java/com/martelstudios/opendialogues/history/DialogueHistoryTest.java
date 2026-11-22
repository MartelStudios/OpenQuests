package com.martelstudios.opendialogues.history;

import com.martelstudios.opendialogues.CodecJson;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    @Test
    void countsAreKeptPerCharacter() {
        DialogueHistory history = new DialogueHistory();
        history.recordOpened("Farmer", "Greeting");
        history.recordOpened("Farmer", "Greeting");
        history.recordOpened("Mage", "Greeting");
        history.recordOpened(null, "Greeting");
        history.recordAnswer("Farmer", "Greeting", "Bye");

        assertEquals(4, history.opened("Greeting"));
        assertEquals(2, history.count("Farmer", "Greeting", null));
        assertEquals(2, history.count("Farmer", null, null));
        assertEquals(4, history.count(null, null, null));
        assertEquals(1, history.count("Farmer", null, "Bye"));
        assertEquals(1, history.count(null, null, "Bye"));
        assertEquals(0, history.count("Mage", null, "Bye"));
    }

    @Test
    void perCharacterCountsSurviveACodecRoundTrip() {
        DialogueHistory history = new DialogueHistory();
        history.recordOpened("Farmer", "Greeting");
        history.recordAnswer(null, "Greeting", "Bye");

        DialogueHistory read = CodecJson.roundTrip(DialogueHistory.CODEC, history);

        assertEquals(1, read.count("Farmer", "Greeting", null));
        assertEquals(1, read.count(null, "Greeting", "Bye"));
    }

    @Test
    void anOldHistoryLoadsWithoutCharacter() {
        DialogueHistory read = CodecJson.decodeUnvalidated(DialogueHistory.CODEC, "{\"Dialogues\":{\"Greeting\":{\"Opened\":2,\"Answers\":{\"Bye\":1}}}}");

        assertEquals(2, read.opened("Greeting"));
        assertEquals(1, read.answered("Greeting", "Bye"));
        assertEquals(0, read.count("Farmer", null, null));
        assertNull(read.lastOccurrence(null, "Greeting", null));
        assertNull(read.lastOccurrence(null, "Greeting", "Bye"));
        var now = Instant.EPOCH;
        read.recordOpened(null, "Greeting", now);
        assertEquals(3, read.opened("Greeting"));
        assertEquals(now, read.lastOccurrence(null, "Greeting", null));
    }

    @Test
    void clearingADialogueClearsItUnderEveryCharacter() {
        DialogueHistory history = new DialogueHistory();
        history.recordOpened("Farmer", "Greeting");
        history.recordOpened(null, "Greeting");
        history.recordOpened("Farmer", "Harvest");

        assertTrue(history.clear("Greeting"));
        assertEquals(0, history.opened("Greeting"));
        assertEquals(1, history.count("Farmer", "Harvest", null));
    }

    @Test
    void latestOccurrenceSurvivesSaveAndIsScoped() {
        var older = Instant.ofEpochMilli(1000);
        var newer = Instant.ofEpochMilli(2000);
        var history = new DialogueHistory();
        history.recordOpened("A", "Greeting", newer);
        history.recordOpened("A", "Greeting", older);
        history.recordAnswer("A", "Greeting", "Accept", older);
        history.recordAnswer("B", "Greeting", "Accept", newer);
        history.recordAnswer("A", "Greeting", "Decline", newer);

        var read = CodecJson.roundTrip(DialogueHistory.CODEC, history);
        assertEquals(newer, read.lastOccurrence(null, "Greeting", null));
        assertEquals(2, read.opened("Greeting"));
        assertEquals(newer, read.lastOccurrence(null, "Greeting", "Accept"));
        assertEquals(older, read.lastOccurrence("A", "Greeting", "Accept"));
        assertNull(read.lastOccurrence("B", "Greeting", "Decline"));
        var copy = (DialogueHistory) read.clone();
        copy.recordAnswer("A", "Greeting", "Accept", newer);
        assertEquals(older, read.lastOccurrence("A", "Greeting", "Accept"));
        copy.clear("Greeting");
        assertNull(copy.lastOccurrence(null, "Greeting", "Accept"));
    }

    private static final UUID A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID B = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    private static DialogueHistory openedWithTwoNpcs() {
        DialogueHistory history = new DialogueHistory();
        history.recordOpened("Farmer", A, "Intro", Instant.ofEpochMilli(1000));
        history.recordOpened("Farmer", B, "Intro", Instant.ofEpochMilli(2000));
        history.recordOpened("Farmer", B, "Intro", Instant.ofEpochMilli(3000));
        history.recordAnswer("Farmer", A, "Intro", "Accept", Instant.ofEpochMilli(1500));
        return history;
    }

    @Test
    void npcCountsAreKeptApart() {
        DialogueHistory history = openedWithTwoNpcs();

        assertEquals(1, history.count("Farmer", A, "Intro", null));
        assertEquals(2, history.count("Farmer", B, "Intro", null));
        assertEquals(3, history.count("Farmer", null, "Intro", null));
        assertEquals(1, history.count("Farmer", A, "Intro", "Accept"));
        assertEquals(0, history.count("Farmer", B, "Intro", "Accept"));
    }

    @Test
    void npcLevelSurvivesACodecRoundTrip() {
        DialogueHistory read = CodecJson.roundTrip(DialogueHistory.CODEC, openedWithTwoNpcs());

        assertEquals(1, read.count("Farmer", A, "Intro", null));
        assertEquals(2, read.count("Farmer", B, "Intro", null));
        assertEquals(3, read.count("Farmer", null, "Intro", null));
        assertEquals(Instant.ofEpochMilli(1000), read.lastOccurrence("Farmer", A, "Intro", null));
        assertEquals(Instant.ofEpochMilli(3000), read.lastOccurrence("Farmer", null, "Intro", null));
        assertEquals(Instant.ofEpochMilli(1500), read.lastOccurrence("Farmer", A, "Intro", "Accept"));
        assertNull(read.lastOccurrence("Farmer", B, "Intro", "Accept"));
    }

    @Test
    void historySavedWithoutNpcCountsForAnyNpcOnly() {
        DialogueHistory read = CodecJson.decodeUnvalidated(DialogueHistory.CODEC, "{\"Characters\":{\"Farmer\":{\"Intro\":{\"Opened\":2}}}}");

        assertEquals(2, read.count("Farmer", null, "Intro", null));
        assertEquals(0, read.count("Farmer", A, "Intro", null));
    }

    @Test
    void forgettingAnNpcKeepsTheOthers() {
        DialogueHistory history = openedWithTwoNpcs();

        assertTrue(history.forgetNpc(A));
        assertEquals(0, history.count("Farmer", A, "Intro", null));
        assertEquals(2, history.count("Farmer", B, "Intro", null));
        assertFalse(history.forgetNpc(A));
    }

    @Test
    void aCloneKeepsNpcsApart() {
        DialogueHistory history = openedWithTwoNpcs();

        DialogueHistory copy = (DialogueHistory) history.clone();
        copy.recordOpened("Farmer", A, "Intro", Instant.EPOCH);

        assertEquals(1, history.count("Farmer", A, "Intro", null));
        assertEquals(2, copy.count("Farmer", A, "Intro", null));
    }
}
