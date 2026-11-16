package com.martelstudios.openquests.dialogues.quests.dialogue;

import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.codec.util.RawJsonReader;
import com.martelstudios.opendialogues.characters.OpenDialogueCharacterAsset;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CharacterNameTest {

    private static final String SOMEONE = "openquestsdialogues.quest.default.someone";

    private static OpenDialogueCharacterAsset character(String json) throws IOException {
        try (RawJsonReader reader = RawJsonReader.fromBuffer(json.toCharArray())) {
            return OpenDialogueCharacterAsset.CODEC.decodeJson(reader, new ExtraInfo());
        }
    }

    @Test
    void name_comes_first() throws IOException {
        assertEquals("farmer.name", DialogueQuestProgression.characterNameKey(character("{\"Name\":\"farmer.name\",\"Epithet\":\"farmer.epithet\"}")));
    }

    @Test
    void epithet_without_name() throws IOException {
        assertEquals("farmer.epithet", DialogueQuestProgression.characterNameKey(character("{\"Epithet\":\"farmer.epithet\"}")));
    }

    @Test
    void someone_without_either() throws IOException {
        assertEquals(SOMEONE, DialogueQuestProgression.characterNameKey(character("{}")));
    }

    @Test
    void someone_for_unknown_character() {
        assertEquals(SOMEONE, DialogueQuestProgression.characterNameKey(null));
    }
}
