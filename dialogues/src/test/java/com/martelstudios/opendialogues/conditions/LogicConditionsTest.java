package com.martelstudios.opendialogues.conditions;

import com.martelstudios.opendialogues.history.DialogueHistory;
import org.junit.jupiter.api.Test;

import static com.martelstudios.opendialogues.conditions.ConditionFixtures.condition;
import static com.martelstudios.opendialogues.conditions.ConditionFixtures.context;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogicConditionsTest {

    private static final String OPENED = "{ \"Type\": \"Opened\", \"Dialogue\": \"Elder_Welcome\" }";
    private static final String NOT_OPENED = "{ \"Type\": \"Opened\", \"Dialogue\": \"Elder_Welcome\", \"Not\": true }";

    @Test
    void andOfNothingHolds() {
        assertTrue(condition("{ \"Type\": \"Composite\", \"Conditions\": [] }").test(context(new DialogueHistory())));
    }

    @Test
    void orOfNothingFails() {
        assertFalse(condition("{ \"Type\": \"Composite\", \"Operator\": \"Or\", \"Conditions\": [] }").test(context(new DialogueHistory())));
    }

    @Test
    void andNeedsEveryCondition() {
        DialogueCondition and = condition("{ \"Type\": \"Composite\", \"Conditions\": [ " + NOT_OPENED + ", " + OPENED + " ] }");

        assertFalse(and.test(context(new DialogueHistory())));
    }

    @Test
    void orHoldsWhenOneConditionHolds() {
        DialogueHistory history = new DialogueHistory();
        history.recordOpened("Elder_Welcome");

        DialogueCondition or = condition("{ \"Type\": \"Composite\", \"Operator\": \"Or\", \"Conditions\": [ " + NOT_OPENED + ", " + OPENED + " ] }");

        assertTrue(or.test(context(history)));
        assertTrue(or.test(context(new DialogueHistory())));
    }

    @Test
    void notOrHoldsWhenNoneHolds() {
        DialogueCondition none = condition("{ \"Type\": \"Composite\", \"Operator\": \"Or\", \"Not\": true, \"Conditions\": [ " + OPENED + " ] }");
        DialogueHistory history = new DialogueHistory();

        assertTrue(none.test(context(history)));

        history.recordOpened("Elder_Welcome");
        assertFalse(none.test(context(history)));
    }

    @Test
    void notFieldInvertsAnyCondition() {
        DialogueCondition notOpened = condition(NOT_OPENED);
        DialogueHistory history = new DialogueHistory();

        assertTrue(notOpened.test(context(history)));

        history.recordOpened("Elder_Welcome");
        assertFalse(notOpened.test(context(history)));
    }
}
