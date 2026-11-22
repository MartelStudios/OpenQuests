package com.martelstudios.openquests.dialogues.givers;

import com.martelstudios.opendialogues.sessions.DialogueSource;
import com.martelstudios.openquests.core.persistence.CodecJson;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CharacterGiverTest {

    private static final UUID NPC = UUID.randomUUID();
    private static final Instant AT = Instant.ofEpochMilli(5_000);

    @Test
    void theSourceGivesTheGiver() {
        assertEquals(new CharacterGiver("Farmer", NPC, "names.mira", AT), CharacterGiver.of(new DialogueSource(NPC, "Farmer", "names.mira", "farmers.epithet", AT)));
    }

    @Test
    void theEpithetNamesANamelessGiver() {
        assertEquals("farmers.epithet", CharacterGiver.of(new DialogueSource(NPC, "Farmer", null, "farmers.epithet")).getNameKey());
    }

    @Test
    void noSourceAndGiverlessQuestsHaveNoGiver() {
        assertNull(CharacterGiver.of((DialogueSource) null));
        assertNull(CharacterGiver.of(new TestQuest()));
    }

    @Test
    void stepsReadTheGiverOfTheirHead() {
        CharacterGiver giver = new CharacterGiver("Farmer", NPC, "names.mira");
        TestQuest.Group head = TestQuest.givenBy(new TestQuest.Group(), giver);

        assertEquals(giver, CharacterGiver.of(new TestQuest().stepOf(head)));
    }

    @Test
    void equalByValue() {
        assertEquals(new CharacterGiver("Farmer", NPC, "a", AT), new CharacterGiver("Farmer", NPC, "a", AT));
        assertEquals(new CharacterGiver("Farmer", NPC, "a", AT).hashCode(), new CharacterGiver("Farmer", NPC, "a", AT).hashCode());
        assertNotEquals(new CharacterGiver("Farmer", NPC, "a", AT), new CharacterGiver("Smith", NPC, "a", AT));
    }

    @Test
    void aGiverMissingItsCharacterOrNpcIsNoGiver() {
        CharacterGiver.register();
        TestQuest quest = TestQuest.givenBy(new TestQuest(), new CharacterGiver("Farmer", NPC, "names.mira", AT));
        String json = CodecJson.encode(TestQuest.CODEC, quest);

        TestQuest noCharacter = CodecJson.decode(TestQuest.CODEC, json.replace("\"Character\":", "\"Dropped\":"), "quest");
        TestQuest noNpc = CodecJson.decode(TestQuest.CODEC, json.replace("\"Npc\"", "\"Dropped\""), "quest");

        assertNull(CharacterGiver.of(noCharacter));
        assertNull(CharacterGiver.of(noNpc));
    }

    @Test
    void roundTrip() {
        CharacterGiver.register();
        TestQuest quest = TestQuest.givenBy(new TestQuest(), new CharacterGiver("Farmer", NPC, "names.mira", AT));
        String json = CodecJson.encode(TestQuest.CODEC, quest);

        TestQuest read = CodecJson.decode(TestQuest.CODEC, json, "quest");

        assertEquals(quest.getGiver(), read.getGiver());
        assertTrue(json.contains("\"" + CharacterGiver.TYPE_ID + "\""));
        assertFalse(read.getTags().containsKey("OpenQuestsDialogues.GiverNpc"));
    }
}
