package com.martelstudios.openquests.dialogues.quests.dialogue;

import com.martelstudios.opendialogues.history.DialogueHistory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenQuestsDialogueAssetTest {

    private static OpenQuestsDialogueAsset asset(String dialogue, String answer) {
        OpenQuestsDialogueAsset asset = new OpenQuestsDialogueAsset();
        asset.dialogue = dialogue;
        asset.answer = answer;
        return asset;
    }

    @Test
    void opening_matches_without_answer() {
        OpenQuestsDialogueAsset asset = asset("Elder", null);

        assertTrue(asset.matches("Elder", null));
        assertFalse(asset.matches("Elder", "Thanks"));
        assertFalse(asset.matches("Other", null));
    }

    @Test
    void answer_matches_only_its_key() {
        OpenQuestsDialogueAsset asset = asset("Elder", "Thanks");

        assertTrue(asset.matches("Elder", "Thanks"));
        assertFalse(asset.matches("Elder", null));
        assertFalse(asset.matches("Elder", "2"));
    }

    @Test
    void counts_history() {
        DialogueHistory history = new DialogueHistory();
        history.recordOpened("Elder");
        history.recordOpened("Elder");
        history.recordAnswer("Elder", "Thanks");

        assertEquals(2, asset("Elder", null).countIn(history));
        assertEquals(1, asset("Elder", "Thanks").countIn(history));
        assertEquals(0, asset("Other", null).countIn(history));
    }
}
