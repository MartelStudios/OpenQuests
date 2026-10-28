package com.martelstudios.opendialogues.triggers;

import com.martelstudios.opendialogues.CodecJson;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OpenDialoguesOpenInteractionTest {

    @Test
    void aDialogueIdIsRead() {
        OpenDialoguesOpenInteraction interaction = CodecJson.decode(OpenDialoguesOpenInteraction.CODEC, "{ \"Dialogue\": \"OpenDialogues_ExampleElder\" }");

        assertNotNull(interaction);
        assertEquals("OpenDialogues_ExampleElder", interaction.getDialogue());
    }
}
