package com.martelstudios.opendialogues.conditions;

import com.martelstudios.opendialogues.CodecJson;
import com.martelstudios.opendialogues.dialogues.OpenDialogueAsset;
import com.martelstudios.opendialogues.history.DialogueHistory;
import com.martelstudios.opendialogues.sessions.DialogueSource;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static com.martelstudios.opendialogues.conditions.ConditionFixtures.condition;
import static com.martelstudios.opendialogues.conditions.ConditionFixtures.context;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HistoryConditionsTest {

    @Test
    void openedHoldsFromMinOpenings() {
        DialogueCondition opened = condition("{ \"Type\": \"Opened\", \"Dialogue\": \"Elder_Welcome\", \"Min\": 2 }");
        DialogueHistory history = new DialogueHistory();

        history.recordOpened("Elder_Welcome");
        assertFalse(opened.test(context(history)));

        history.recordOpened("Elder_Welcome");
        assertTrue(opened.test(context(history)));
    }

    @Test
    void openedStopsHoldingPastMax() {
        DialogueCondition opened = condition("{ \"Type\": \"Opened\", \"Dialogue\": \"Elder_Welcome\", \"Max\": 1 }");
        DialogueHistory history = new DialogueHistory();

        history.recordOpened("Elder_Welcome");
        assertTrue(opened.test(context(history)));

        history.recordOpened("Elder_Welcome");
        assertFalse(opened.test(context(history)));
    }

    @Test
    void validationRejectsMaxBelowMin() {
        assertNotNull(condition("{ \"Type\": \"Opened\", \"Dialogue\": \"Elder_Welcome\", \"Min\": 2, \"Max\": 1 }").validate(id -> null));
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
    void openedValidationNamesAMissingDialogue() {
        DialogueCondition opened = condition("{ \"Type\": \"Opened\", \"Dialogue\": \"Elder_Welcome\" }");

        assertNotNull(opened.validate(id -> null));
    }

    @Test
    void openedAfterComparesTheLatestOpenings() {
        DialogueCondition after = condition("{ \"Type\": \"OpenedAfter\", \"Dialogue\": \"Intro\", \"Than\": \"Thanks\" }");
        DialogueHistory history = new DialogueHistory();
        assertFalse(after.test(context(history)));

        history.recordOpened(null, "Intro", Instant.ofEpochSecond(10));
        assertTrue(after.test(context(history)));

        history.recordOpened(null, "Thanks", Instant.ofEpochSecond(20));
        assertFalse(after.test(context(history)));

        history.recordOpened(null, "Intro", Instant.ofEpochSecond(30));
        assertTrue(after.test(context(history)));
    }

    @Test
    void openedAfterValidationNamesAMissingDialogue() {
        OpenDialogueAsset intro = CodecJson.decode(OpenDialogueAsset.BODY_CODEC, "{ \"Responses\": [ { \"Text\": \"a\", \"Id\": \"Accept\" } ] }");
        Map<String, OpenDialogueAsset> dialogues = Map.of("Intro", intro);

        assertNotNull(condition("{ \"Type\": \"OpenedAfter\", \"Dialogue\": \"Intro\", \"Than\": \"Nowhere\" }").validate(dialogues::get));
        assertNull(condition("{ \"Type\": \"OpenedAfter\", \"Dialogue\": \"Intro\", \"Than\": \"Intro\" }").validate(dialogues::get));
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
        DialogueCondition composite = condition("{ \"Type\": \"Composite\", \"Conditions\": [ { \"Type\": \"Opened\", \"Dialogue\": \"Elder_Welcome\" } ] }");

        assertNotNull(composite.validate(id -> null));
    }

    private static final DialogueSource FARMER_A = new DialogueSource(UUID.randomUUID(), "Farmer", "npc.a", null);
    private static final DialogueSource FARMER_B = new DialogueSource(UUID.randomUUID(), "Farmer", "npc.b", null);

    @Test
    void openedWithNpcCountsOnlyThatNpc() {
        DialogueCondition opened = condition("{ \"Type\": \"Opened\", \"Dialogue\": \"Intro\", \"Min\": 1, \"With\": \"Npc\" }");
        DialogueHistory history = new DialogueHistory();
        history.recordOpened("Farmer", FARMER_A.npc(), "Intro", Instant.now());

        assertTrue(opened.test(context(history, FARMER_A)));
        assertFalse(opened.test(context(history, FARMER_B)));
    }

    @Test
    void answeredWithCharacterCountsEveryNpcOfIt() {
        DialogueCondition answered = condition("{ \"Type\": \"Answered\", \"Dialogue\": \"Intro\", \"Answer\": \"Accept\", \"With\": \"Character\" }");
        DialogueHistory history = new DialogueHistory();
        history.recordAnswer("Farmer", FARMER_A.npc(), "Intro", "Accept", Instant.now());

        assertTrue(answered.test(context(history, FARMER_B)));
        assertFalse(answered.test(context(history, new DialogueSource(UUID.randomUUID(), "Smith", null, null))));
    }

    @Test
    void scopedConditionWithoutSourceJudgesAnEmptyHistory() {
        DialogueHistory history = new DialogueHistory();
        history.recordOpened("Farmer", FARMER_A.npc(), "Intro", Instant.now());

        assertTrue(condition("{ \"Type\": \"Opened\", \"Dialogue\": \"Intro\", \"Min\": 0, \"Max\": 0, \"With\": \"Npc\" }").test(context(history)));
        assertFalse(condition("{ \"Type\": \"Opened\", \"Dialogue\": \"Intro\", \"Min\": 1, \"With\": \"Npc\" }").test(context(history)));
        assertFalse(condition("{ \"Type\": \"Opened\", \"Dialogue\": \"Intro\", \"Min\": 1, \"With\": \"Character\" }").test(context(history)));
    }

    @Test
    void withDefaultsToAny() {
        DialogueCondition opened = condition("{ \"Type\": \"Opened\", \"Dialogue\": \"Intro\", \"Min\": 2 }");
        DialogueHistory history = new DialogueHistory();
        history.recordOpened("Farmer", FARMER_A.npc(), "Intro", Instant.now());
        history.recordOpened("Intro");

        assertTrue(opened.test(context(history, FARMER_B)));
        assertTrue(opened.test(context(history)));
    }
}
