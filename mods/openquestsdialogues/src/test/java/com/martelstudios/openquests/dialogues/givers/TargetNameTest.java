package com.martelstudios.openquests.dialogues.givers;

import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.dialogues.quests.dialogue.DialogueQuestProgression;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TargetNameTest {

    @Test
    void theGiverNameIsTheTargetName() {
        TestQuest quest = TestQuest.givenBy(new TestQuest(), new CharacterGiver("Farmer", UUID.randomUUID(), "names.mira"));

        assertEquals("names.mira", DialogueQuestProgression.targetName(quest, TargetMode.GiverNpc, null).getFormattedMessage().messageId);
    }

    @Test
    void aTextGiverTagNamesNoOne() {
        TestQuest quest = new TestQuest();
        quest.apply(new AbstractQuestProgression.PutTag("Text.giver", new String[] {"farmers.someone"}));

        assertNull(DialogueQuestProgression.targetName(quest, TargetMode.GiverNpc, null));
    }
}
