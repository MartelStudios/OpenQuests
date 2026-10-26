package com.martelstudios.opendialogues.conditions;

import com.martelstudios.opendialogues.CodecJson;
import com.martelstudios.opendialogues.dialogues.OpenDialogueAsset;
import com.martelstudios.opendialogues.history.DialogueHistory;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.martelstudios.opendialogues.conditions.ConditionFixtures.condition;
import static com.martelstudios.opendialogues.conditions.ConditionFixtures.context;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HistoryConditionsTest {

    @Test
    void seenHoldsFromMinOpenings() {
        DialogueCondition seen = condition("{ \"Type\": \"Seen\", \"Dialogue\": \"Elder_Welcome\", \"Min\": 2 }");
        DialogueHistory history = new DialogueHistory();

        history.recordOpened("Elder_Welcome");
        assertFalse(seen.test(context(history)));

        history.recordOpened("Elder_Welcome");
        assertTrue(seen.test(context(history)));
    }

    @Test
    void answeredReadsTheAnswerKey() {
        DialogueCondition answered = condition("{ \"Type\": \"Answered\", \"Dialogue\": \"Elder_Flowers\", \"Answer\": \"Accept\" }");
        DialogueHistory history = new DialogueHistory();

        history.recordAnswer("Elder_Flowers", "Refuse");
        assertFalse(answered.test(context(history)));

        history.recordAnswer("Elder_Flowers", "Accept");
        assertTrue(answered.test(context(history)));
    }

    @Test
    void seenValidationNamesAMissingDialogue() {
        DialogueCondition seen = condition("{ \"Type\": \"Seen\", \"Dialogue\": \"Elder_Welcome\" }");

        assertNotNull(seen.validate(id -> null));
    }

    @Test
    void answeredValidationNamesAMissingAnswer() {
        OpenDialogueAsset flowers = CodecJson.decode(OpenDialogueAsset.BODY_CODEC, "{ \"Responses\": [ { \"Text\": \"a\", \"Id\": \"Accept\" } ] }");
        Map<String, OpenDialogueAsset> dialogues = Map.of("Elder_Flowers", flowers);

        assertNull(condition("{ \"Type\": \"Answered\", \"Dialogue\": \"Elder_Flowers\", \"Answer\": \"Accept\" }").validate(dialogues::get));
        assertNotNull(condition("{ \"Type\": \"Answered\", \"Dialogue\": \"Elder_Flowers\", \"Answer\": \"Refuse\" }").validate(dialogues::get));
    }

    @Test
    void validationReachesNestedConditions() {
        DialogueCondition composite = condition("{ \"Type\": \"Composite\", \"Conditions\": [ { \"Type\": \"Seen\", \"Dialogue\": \"Elder_Welcome\" } ] }");

        assertNotNull(composite.validate(id -> null));
    }
}
