package com.martelstudios.opendialogues.conditions.impl.history;

import com.martelstudios.opendialogues.CodecJson;
import com.martelstudios.opendialogues.dialogues.OpenDialogueAsset;
import com.martelstudios.opendialogues.history.DialogueHistory;
import com.martelstudios.opendialogues.sessions.DialogueSource;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenDialoguesCooldownConditionTest {

    @Test
    void onlyMatchingOccurrencesStartOrExtendTheCooldown() {
        var condition = CodecJson.decode(OpenDialoguesCooldownCondition.CODEC,
                """
                {"Dialogue":"Greeting","Character":"Elder","Answer":"Accept","Seconds":10}
                """);
        var history = new DialogueHistory();
        Instant start = Instant.EPOCH;
        assertFalse(condition.isActive(history, start));
        history.recordOpened("Elder", "Greeting", start);
        history.recordAnswer("Other", "Greeting", "Accept", start);
        history.recordAnswer("Elder", "Greeting", "Decline", start);
        assertFalse(condition.isActive(history, start));
        history.recordAnswer("Elder", "Greeting", "Accept", start);
        assertTrue(condition.isActive(history, start.plusMillis(9_999)));
        assertFalse(condition.isActive(history, start.plusSeconds(10)));
        history.recordAnswer("Elder", "Greeting", "Accept", start.plusSeconds(8));
        assertTrue(condition.isActive(history, start.plusSeconds(10)));
    }

    @Test
    void validationRejectsUnknownCharacters() {
        var condition = CodecJson.decode(OpenDialoguesCooldownCondition.CODEC,
                """
                {"Dialogue":"Greeting","Character":"Missing","Seconds":10}
                """);
        var dialogue = CodecJson.decode(OpenDialogueAsset.BODY_CODEC, "{}");
        assertEquals("No character 'Missing'", condition.validate(id -> dialogue, id -> null));
    }

    @Test
    void validationRejectsUnknownAnswersAndNonPositiveDuration() {
        var condition = CodecJson.decode(OpenDialoguesCooldownCondition.CODEC,
                """
                {"Dialogue":"Greeting","Answer":"Missing","Seconds":10}
                """);
        var dialogue = CodecJson.decode(OpenDialogueAsset.BODY_CODEC, "{}");
        assertNotNull(condition.validate(id -> dialogue));
        var zero = CodecJson.decodeUnvalidated(OpenDialoguesCooldownCondition.CODEC,
                """
                {"Dialogue":"Greeting","Seconds":0}
                """);
        assertNotNull(zero.validate(id -> dialogue));
    }

    @Test
    void cooldownWithNpcRunsPerNpc() {
        var condition = CodecJson.decode(OpenDialoguesCooldownCondition.CODEC,
                """
                {"Dialogue":"Intro","With":"Npc","Seconds":60}
                """);
        var a = new DialogueSource(UUID.randomUUID(), "Farmer", null, null);
        var b = new DialogueSource(UUID.randomUUID(), "Farmer", null, null);
        var history = new DialogueHistory();
        Instant now = Instant.EPOCH;
        history.recordOpened("Farmer", a.npc(), "Intro", now);

        assertTrue(condition.isActive(history, a, now));
        assertFalse(condition.isActive(history, b, now));
        assertFalse(condition.isActive(history, null, now));
    }

    @Test
    void cooldownRejectsWithAndCharacterTogether() {
        var condition = CodecJson.decodeUnvalidated(OpenDialoguesCooldownCondition.CODEC,
                """
                {"Dialogue":"Intro","Character":"Farmer","With":"Npc","Seconds":60}
                """);
        var dialogue = CodecJson.decode(OpenDialogueAsset.BODY_CODEC, "{}");

        assertNotNull(condition.validate(id -> dialogue, id -> null));
    }
}
