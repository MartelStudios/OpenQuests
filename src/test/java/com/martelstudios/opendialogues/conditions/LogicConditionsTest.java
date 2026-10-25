package com.martelstudios.opendialogues.conditions;

import com.martelstudios.opendialogues.history.DialogueHistory;
import org.junit.jupiter.api.Test;

import static com.martelstudios.opendialogues.conditions.ConditionFixtures.condition;
import static com.martelstudios.opendialogues.conditions.ConditionFixtures.context;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogicConditionsTest {

    private static final String SEEN = "{ \"Type\": \"OpenDialoguesSeen\", \"Dialogue\": \"Elder_Welcome\" }";

    @Test
    void allOfNothingHolds() {
        assertTrue(condition("{ \"Type\": \"OpenDialoguesAll\", \"Conditions\": [] }").test(context(new DialogueHistory())));
    }

    @Test
    void anyOfNothingFails() {
        assertFalse(condition("{ \"Type\": \"OpenDialoguesAny\", \"Conditions\": [] }").test(context(new DialogueHistory())));
    }

    @Test
    void anyHoldsWhenOneConditionHolds() {
        DialogueHistory history = new DialogueHistory();
        history.recordOpened("Elder_Welcome");

        DialogueCondition any = condition("{ \"Type\": \"OpenDialoguesAny\", \"Conditions\": [ { \"Type\": \"OpenDialoguesNot\", \"Condition\": " + SEEN + " }, " + SEEN + " ] }");

        assertTrue(any.test(context(history)));
        assertTrue(any.test(context(new DialogueHistory())));
    }

    @Test
    void notInverts() {
        DialogueCondition not = condition("{ \"Type\": \"OpenDialoguesNot\", \"Condition\": " + SEEN + " }");

        assertTrue(not.test(context(new DialogueHistory())));
    }
}
