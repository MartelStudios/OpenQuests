package com.martelstudios.opendialogues.conditions.impl.world;

import com.martelstudios.opendialogues.CodecJson;
import com.martelstudios.opendialogues.conditions.ConditionFixtures;
import com.martelstudios.opendialogues.conditions.DialogueCondition;
import com.martelstudios.opendialogues.history.DialogueHistory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class NearNpcConditionTest {

    @BeforeAll
    static void register() {
        ConditionFixtures.registerConditions();
    }

    @Test
    void aGroupIsEnough() {
        assertNotNull(decode("{ \"Type\": \"NearNpc\", \"Group\": \"Spiders\", \"Radius\": 100, \"Min\": 3 }"));
    }

    @Test
    void roleOrGroupIsRequired() {
        assertNull(decode("{ \"Type\": \"NearNpc\", \"Radius\": 10 }"));
    }

    @Test
    void roleAndGroupTogetherAreRefused() {
        assertNull(decode("{ \"Type\": \"NearNpc\", \"Role\": \"Spider\", \"Group\": \"Spiders\", \"Radius\": 10 }"));
    }

    @Test
    void radiusAboveCapIsRefused() {
        assertNull(decode("{ \"Type\": \"NearNpc\", \"Role\": \"Spider\", \"Radius\": 129 }"));
    }

    @Test
    void radiusMustBePositive() {
        assertNull(decode("{ \"Type\": \"NearNpc\", \"Role\": \"Spider\", \"Radius\": 0 }"));
    }

    @Test
    void maxBelowMinIsReported() {
        DialogueCondition condition = decode("{ \"Type\": \"NearNpc\", \"Role\": \"Spider\", \"Radius\": 10, \"Min\": 2, \"Max\": 1 }");

        assertNotNull(condition);
        assertEquals("Max 1 is below Min 2", condition.validate(id -> null));
    }

    @Test
    void falseOffTheWorldThread() {
        DialogueCondition condition = decode("{ \"Type\": \"NearNpc\", \"Role\": \"Spider\", \"Radius\": 10, \"Min\": 0 }");

        assertNotNull(condition);
        assertFalse(condition.test(ConditionFixtures.context(new DialogueHistory())));
    }

    private static DialogueCondition decode(String json) {
        return CodecJson.decode(DialogueCondition.CODEC, json);
    }
}
