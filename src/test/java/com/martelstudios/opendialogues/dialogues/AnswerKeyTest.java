package com.martelstudios.opendialogues.dialogues;

import com.martelstudios.opendialogues.CodecJson;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class AnswerKeyTest {

    private static OpenDialogueAsset dialogue(String responses) {
        return CodecJson.decode(OpenDialogueAsset.BODY_CODEC, "{ \"Responses\": [" + responses + "] }");
    }

    @Test
    void anAnswerWithoutIdIsKeyedByItsPosition() {
        OpenDialogueAsset dialogue = dialogue("{ \"Text\": \"a\", \"Id\": \"Accept\" }, { \"Text\": \"b\" }");

        assertNotNull(dialogue);
        assertEquals("2", dialogue.answerKey(dialogue.getResponses()[1]));
    }

    @Test
    void anAnswerIdIsItsKey() {
        OpenDialogueAsset dialogue = dialogue("{ \"Text\": \"a\", \"Id\": \"Accept\" }");

        assertNotNull(dialogue);
        assertEquals("Accept", dialogue.answerKey(dialogue.getResponses()[0]));
        assertSame(dialogue.getResponses()[0], dialogue.findAnswer("Accept"));
        assertNull(dialogue.findAnswer("Refuse"));
    }

    @Test
    void twoAnswersSharingAKeyAreRefused() {
        assertNull(dialogue("{ \"Text\": \"a\", \"Id\": \"Accept\" }, { \"Text\": \"b\", \"Id\": \"Accept\" }"));
    }

    @Test
    void aNumericAnswerIdIsRefused() {
        assertNull(dialogue("{ \"Text\": \"a\", \"Id\": \"2\" }, { \"Text\": \"b\" }"));
    }
}
