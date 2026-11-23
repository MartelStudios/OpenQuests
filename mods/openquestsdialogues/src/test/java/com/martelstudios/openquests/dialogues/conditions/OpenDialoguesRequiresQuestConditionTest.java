package com.martelstudios.openquests.dialogues.conditions;

import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.codec.util.RawJsonReader;
import com.martelstudios.opendialogues.conditions.SourceScope;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class OpenDialoguesRequiresQuestConditionTest {

    /**
     * Decodes without running validators: the asset id check needs the quest store loaded.
     */
    private static OpenDialoguesRequiresQuestCondition decode(String json) throws IOException {
        try (RawJsonReader reader = RawJsonReader.fromBuffer(json.toCharArray())) {
            return OpenDialoguesRequiresQuestCondition.CODEC.decodeJson(reader, new ExtraInfo());
        }
    }

    @Test
    void decodes_requirement_fields() throws IOException {
        OpenDialoguesRequiresQuestCondition condition = decode("{\"QuestAssetId\":\"ElderPotion\",\"States\":[\"InProgress\"]}");

        assertEquals("ElderPotion", condition.getRequirement().getQuestAssetId());
        assertNull(condition.validate(id -> null));
    }

    @Test
    void decodesAList() throws IOException {
        assertEquals(List.of("A", "B"), decode("{\"QuestAssetIds\":[\"A\",\"B\"],\"GivenBy\":\"Npc\"}").getRequirement().getQuestAssetIds());
    }

    @Test
    void givenByDefaultsToAny() throws IOException {
        assertEquals(SourceScope.Any, decode("{\"QuestAssetId\":\"ElderPotion\"}").givenBy);
        assertEquals(SourceScope.Npc, decode("{\"QuestAssetId\":\"ElderPotion\",\"GivenBy\":\"Npc\"}").givenBy);
    }

    @Test
    void bounds_error_reported() throws IOException {
        OpenDialoguesRequiresQuestCondition condition = decode("{\"QuestAssetId\":\"ElderPotion\",\"Min\":2,\"Max\":1}");

        assertNotNull(condition.validate(id -> null));
    }
}
