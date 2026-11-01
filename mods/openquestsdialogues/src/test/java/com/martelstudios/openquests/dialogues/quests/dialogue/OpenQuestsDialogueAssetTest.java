package com.martelstudios.openquests.dialogues.quests.dialogue;

import com.martelstudios.opendialogues.history.DialogueHistory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenQuestsDialogueAssetTest {

    private static OpenQuestsDialogueAsset asset(String character, String dialogue, String answer) {
        OpenQuestsDialogueAsset asset = new OpenQuestsDialogueAsset();
        asset.character = character;
        asset.dialogue = dialogue;
        asset.answer = answer;
        return asset;
    }

    @Test
    void opening_matches_without_answer() {
        OpenQuestsDialogueAsset asset = asset(null, "Elder", null);

        assertTrue(asset.matches(null, "Elder", null));
        assertFalse(asset.matches(null, "Elder", "Thanks"));
        assertFalse(asset.matches(null, "Other", null));
    }

    @Test
    void answer_matches_only_its_key() {
        OpenQuestsDialogueAsset asset = asset(null, "Elder", "Thanks");

        assertTrue(asset.matches(null, "Elder", "Thanks"));
        assertFalse(asset.matches(null, "Elder", null));
        assertFalse(asset.matches(null, "Elder", "2"));
    }

    @Test
    void counts_history() {
        DialogueHistory history = new DialogueHistory();
        history.recordOpened("Elder");
        history.recordOpened("Elder");
        history.recordAnswer("Elder", "Thanks");

        assertEquals(2, asset(null, "Elder", null).countIn(history));
        assertEquals(1, asset(null, "Elder", "Thanks").countIn(history));
        assertEquals(0, asset(null, "Other", null).countIn(history));
    }

    @Test
    void character_alone_matches_any_opening_with_it() {
        OpenQuestsDialogueAsset asset = asset("Farmer", null, null);

        assertTrue(asset.matches("Farmer", "Greeting", null));
        assertTrue(asset.matches("Farmer", "Harvest", null));
        assertFalse(asset.matches("Mage", "Greeting", null));
        assertFalse(asset.matches(null, "Greeting", null));
        assertFalse(asset.matches("Farmer", "Greeting", "Bye"));
    }

    @Test
    void no_filter_matches_any_opening() {
        OpenQuestsDialogueAsset asset = asset(null, null, null);

        assertTrue(asset.matches(null, "Greeting", null));
        assertTrue(asset.matches("Farmer", "Harvest", null));
        assertFalse(asset.matches("Farmer", "Harvest", "Bye"));
    }

    @Test
    void answer_alone_matches_that_key_in_any_dialogue() {
        OpenQuestsDialogueAsset asset = asset(null, null, "Bye");

        assertTrue(asset.matches(null, "Greeting", "Bye"));
        assertTrue(asset.matches("Farmer", "Harvest", "Bye"));
        assertFalse(asset.matches("Farmer", "Harvest", null));
    }

    @Test
    void all_three_must_match() {
        OpenQuestsDialogueAsset asset = asset("Farmer", "Greeting", "Bye");

        assertTrue(asset.matches("Farmer", "Greeting", "Bye"));
        assertFalse(asset.matches("Mage", "Greeting", "Bye"));
        assertFalse(asset.matches("Farmer", "Harvest", "Bye"));
    }

    @Test
    void counts_history_with_its_filters() {
        DialogueHistory history = new DialogueHistory();
        history.recordOpened("Farmer", "Greeting");
        history.recordOpened("Mage", "Greeting");
        history.recordAnswer("Farmer", "Greeting", "Bye");

        assertEquals(1, asset("Farmer", null, null).countIn(history));
        assertEquals(2, asset(null, null, null).countIn(history));
        assertEquals(1, asset(null, null, "Bye").countIn(history));
    }
}
