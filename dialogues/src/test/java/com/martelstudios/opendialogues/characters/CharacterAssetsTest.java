package com.martelstudios.opendialogues.characters;

import com.martelstudios.opendialogues.CodecJson;
import com.martelstudios.opendialogues.dialogues.traits.OpenDialoguesDialogueTrait;
import com.martelstudios.opendialogues.characters.traits.impl.OpenDialoguesNameTrait;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class CharacterAssetsTest {

    @Test
    void characterDecodesTraits() {
        CharacterFixtures.registerFilters();
        OpenDialogueCharacterAsset character = CodecJson.decode(OpenDialogueCharacterAsset.CODEC, """
            { "Uniqueness": "Alive", "Traits": [
                { "Type": "Dialogue", "Dialogue": { "Dialogue": "Farmer_Hello" }, "Hint": "npc.hint.talk" },
                { "Type": "Name", "Names": "Kweebec_Names" } ] }
            """);

        assertNotNull(character);
        assertEquals(Uniqueness.Alive, character.getUniqueness());
        OpenDialoguesDialogueTrait dialogue = assertInstanceOf(OpenDialoguesDialogueTrait.class, character.getTraits()[0]);
        assertEquals("Farmer_Hello", dialogue.getDialogue().getDialogue());
        assertEquals("npc.hint.talk", dialogue.interactionHint());
        OpenDialoguesNameTrait name = assertInstanceOf(OpenDialoguesNameTrait.class, character.getTraits()[1]);
        assertEquals("Kweebec_Names", name.getNames());
    }

    @Test
    void characterDefaultsToRepeatable() {
        OpenDialogueCharacterAsset character = CodecJson.decode(OpenDialogueCharacterAsset.CODEC, "{}");

        assertNotNull(character);
        assertEquals(Uniqueness.None, character.getUniqueness());
        assertEquals(CharacterScope.World, character.getScope());
    }

    @Test
    void characterDecodesScope() {
        OpenDialogueCharacterAsset character = CodecJson.decode(OpenDialogueCharacterAsset.CODEC, "{ \"Uniqueness\": \"Forever\", \"Scope\": \"Universe\" }");

        assertNotNull(character);
        assertEquals(CharacterScope.Universe, character.getScope());
    }

    @Test
    void textListDecodesKeys() {
        OpenDialogueTextListAsset texts = CodecJson.decode(OpenDialogueTextListAsset.CODEC, "{ \"Texts\": [\"npc.mira\", \"npc.oak\"] }");

        assertNotNull(texts);
        assertEquals(2, texts.getTexts().length);
    }

    @Test
    void distributionEntryWithoutCharacterIsNothing() {
        CharacterFixtures.registerFilters();
        OpenDialogueDistributionAsset distribution = CodecJson.decode(OpenDialogueDistributionAsset.CODEC, """
            { "Filters": [ { "Type": "Group", "Groups": ["Kweebec_Civilian"] } ], "Characters": [ { "Weight": 6 } ] }
            """);

        assertNotNull(distribution);
        assertEquals(1, distribution.getFilters().length);
        assertEquals(6, distribution.getEntries()[0].getWeight());
        assertNull(distribution.getEntries()[0].getCharacter());
    }

    @Test
    void distributionRefusesZeroWeight() {
        assertNull(CodecJson.decode(OpenDialogueDistributionAsset.CODEC, "{ \"Characters\": [ { \"Weight\": 0 } ] }"));
    }
}
