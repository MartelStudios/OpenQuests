package com.martelstudios.openquests.dialogues.givers;

import com.martelstudios.opendialogues.sessions.DialogueSource;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class GiverTest {

    private static final UUID NPC = UUID.randomUUID();

    @Test
    void giverRoundTripsThroughTags() {
        Giver giver = Giver.of(new DialogueSource(NPC, "Farmer", "names.mira", "farmers.epithet"));
        TestQuest quest = new TestQuest();

        giver.writeOn(quest);

        assertEquals(new Giver("Farmer", NPC, "names.mira"), Giver.of(quest));
        assertEquals("names.mira", quest.getTags().get(Giver.NAME_TAG)[0]);
    }

    @Test
    void theEpithetNamesANamelessGiver() {
        assertEquals("farmers.epithet", Giver.of(new DialogueSource(NPC, "Farmer", null, "farmers.epithet")).nameKey());
    }

    @Test
    void namelessGiverWritesNoTextTag() {
        TestQuest quest = new TestQuest();

        Giver.of(new DialogueSource(NPC, "Farmer", null, null)).writeOn(quest);

        assertFalse(quest.getTags().containsKey(Giver.NAME_TAG));
        assertEquals(new Giver("Farmer", NPC, null), Giver.of(quest));
    }

    @Test
    void noSourceAndUntaggedQuestsHaveNoGiver() {
        assertNull(Giver.of((DialogueSource) null));
        assertNull(Giver.of(new TestQuest()));
    }

    @Test
    void stepsReadTheGiverOfTheirHead() {
        TestQuest.Group head = new TestQuest.Group();
        TestQuest step = new TestQuest().stepOf(head);
        Giver giver = new Giver("Farmer", NPC, "names.mira");

        giver.writeOn(head);

        assertEquals(giver, Giver.of(step));
    }
}
