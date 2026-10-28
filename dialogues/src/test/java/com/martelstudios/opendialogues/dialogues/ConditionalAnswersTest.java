package com.martelstudios.opendialogues.dialogues;

import com.martelstudios.opendialogues.CodecJson;
import com.martelstudios.opendialogues.conditions.ConditionFixtures;
import com.martelstudios.opendialogues.history.DialogueHistory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.martelstudios.opendialogues.conditions.ConditionFixtures.context;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConditionalAnswersTest {

    private static final String SEEN_WELCOME = "\"When\": { \"Type\": \"Opened\", \"Dialogue\": \"Elder_Welcome\", \"Min\": 1 }";

    private static OpenDialogueAsset dialogue(String responses) {
        ConditionFixtures.context(new DialogueHistory());
        OpenDialogueAsset dialogue = CodecJson.decode(OpenDialogueAsset.BODY_CODEC, "{ \"Responses\": [" + responses + "] }");
        assertNotNull(dialogue);
        return dialogue;
    }

    private static List<String> texts(List<OpenDialoguesResponse> responses) {
        return responses.stream().map(OpenDialoguesResponse::getText).toList();
    }

    @Test
    void anAnswerWithoutWhenIsAlwaysShown() {
        OpenDialogueAsset dialogue = dialogue("{ \"Text\": \"a\" }");

        assertEquals(1, dialogue.visibleResponses(context(new DialogueHistory())).size());
    }

    @Test
    void anAnswerShowsOnlyWhileItsWhenHolds() {
        OpenDialogueAsset dialogue = dialogue("{ \"Text\": \"a\" }, { \"Text\": \"b\", " + SEEN_WELCOME + " }");
        DialogueHistory history = new DialogueHistory();

        assertEquals(List.of("a"), texts(dialogue.visibleResponses(context(history))));

        history.recordOpened("Elder_Welcome");
        assertEquals(List.of("a", "b"), texts(dialogue.visibleResponses(context(history))));
    }

    @Test
    void notInvertsAnAnswerCondition() {
        OpenDialogueAsset dialogue = dialogue("{ \"Text\": \"a\" }, { \"Text\": \"b\", \"When\": { \"Type\": \"Opened\", \"Dialogue\": \"Elder_Welcome\", \"Min\": 1, \"Not\": true } }");

        assertEquals(List.of("a", "b"), texts(dialogue.visibleResponses(context(new DialogueHistory()))));
    }

    @Test
    void aHiddenAnswerKeepsItsFilePosition() {
        OpenDialogueAsset dialogue = dialogue("{ \"Text\": \"a\" }, { \"Text\": \"b\", " + SEEN_WELCOME + " }, { \"Text\": \"c\" }");

        List<OpenDialoguesResponse> visible = dialogue.visibleResponses(context(new DialogueHistory()));

        assertEquals("3", dialogue.answerKey(visible.get(1)));
    }

    @Test
    void aBrokenAnswerConditionIsReported() {
        OpenDialogueAsset broken = dialogue("{ \"Text\": \"a\", \"When\": { \"Type\": \"Opened\", \"Dialogue\": \"Elder_Welcome\", \"Min\": 3, \"Max\": 1 } }");
        OpenDialogueAsset plain = dialogue("{ \"Text\": \"a\" }");

        String error = broken.findConditionError(id -> null);

        assertNotNull(error);
        assertTrue(error.startsWith("Answer '1': "), error);
        assertNull(plain.findConditionError(id -> null));
    }
}
