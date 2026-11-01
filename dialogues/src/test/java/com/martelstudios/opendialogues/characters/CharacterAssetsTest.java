package com.martelstudios.opendialogues.characters;

import com.martelstudios.opendialogues.CodecJson;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class CharacterAssetsTest {

    @Test
    void characterDecodesEveryField() {
        OpenDialogueCharacterAsset character = CodecJson.decode(OpenDialogueCharacterAsset.CODEC, """
            { "Dialogue": { "Dialogue": "Farmer_Hello" }, "Names": "Kweebec_Names", "Uniqueness": "Alive", "Hint": "npc.hint.talk" }
            """);

        assertNotNull(character);
        assertEquals("Farmer_Hello", character.getDialogue().getDialogue());
        assertEquals("Kweebec_Names", character.getNames());
        assertEquals(Uniqueness.Alive, character.getUniqueness());
        assertEquals("npc.hint.talk", character.getHint());
    }

    @Test
    void characterDefaultsToRepeatable() {
        OpenDialogueCharacterAsset character = CodecJson.decode(OpenDialogueCharacterAsset.CODEC, "{ \"Dialogue\": { \"Router\": \"Farmer\" } }");

        assertNotNull(character);
        assertEquals(Uniqueness.None, character.getUniqueness());
    }

    @Test
    void nameListDecodesKeys() {
        OpenDialogueNameListAsset names = CodecJson.decode(OpenDialogueNameListAsset.CODEC, "{ \"Names\": [\"npc.mira\", \"npc.oak\"] }");

        assertNotNull(names);
        assertEquals(2, names.getNames().length);
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
