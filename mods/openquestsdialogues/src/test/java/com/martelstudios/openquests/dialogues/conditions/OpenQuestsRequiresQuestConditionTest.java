package com.martelstudios.openquests.dialogues.conditions;

import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.codec.util.RawJsonReader;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class OpenQuestsRequiresQuestConditionTest {

    /**
     * Decodes without running validators: the asset id check needs the quest store loaded.
     */
    private static OpenQuestsRequiresQuestCondition decode(String json) throws IOException {
        try (RawJsonReader reader = RawJsonReader.fromBuffer(json.toCharArray())) {
            return OpenQuestsRequiresQuestCondition.CODEC.decodeJson(reader, new ExtraInfo());
        }
    }

    @Test
    void decodes_requirement_fields() throws IOException {
        OpenQuestsRequiresQuestCondition condition = decode("{\"QuestAssetId\":\"ElderPotion\",\"States\":[\"InProgress\"]}");

        assertEquals("ElderPotion", condition.getRequirement().getQuestAssetId());
        assertNull(condition.validate(id -> null));
    }

    @Test
    void bounds_error_reported() throws IOException {
        OpenQuestsRequiresQuestCondition condition = decode("{\"QuestAssetId\":\"ElderPotion\",\"Min\":2,\"Max\":1}");

        assertNotNull(condition.validate(id -> null));
    }
}
