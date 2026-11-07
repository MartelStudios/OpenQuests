package com.martelstudios.opendialogues.dialogues;

import com.martelstudios.opendialogues.CodecJson;
import com.martelstudios.opendialogues.characters.OpenDialogueCharacterAsset;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DialogueCharacterTest {

    @Test
    void dialogueDecodesCharacterAndSourceFlag() {
        OpenDialogueAsset dialogue = DialogueFixtures.dialogue("Elder", "{ \"Character\": \"Villager\", \"UseSourceCharacter\": true }");

        assertEquals("Villager", dialogue.getCharacterId());
        assertTrue(dialogue.isUseSourceCharacter());
    }

    @Test
    void sourceFlagDefaultsToFalse() {
        assertFalse(DialogueFixtures.dialogue("Elder", "{}").isUseSourceCharacter());
    }

    @Test
    void reportsUnknownDialogueCharacter() {
        OpenDialogueAsset dialogue = DialogueFixtures.dialogue("Elder", "{ \"Character\": \"Missing\" }");

        assertEquals("Character 'Missing' is declared by no pack", dialogue.findCharacterError(Map.<String, OpenDialogueCharacterAsset>of()::get));
    }

    @Test
    void knownCharacterIsNoError() {
        OpenDialogueAsset dialogue = DialogueFixtures.dialogue("Elder", "{ \"Character\": \"Villager\" }");

        assertNull(dialogue.findCharacterError(Map.of("Villager", CodecJson.decodeUnvalidated(OpenDialogueCharacterAsset.CODEC, "{}"))::get));
    }
}
