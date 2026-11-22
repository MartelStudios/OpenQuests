package com.martelstudios.openquests.dialogues.conditions;

import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.codec.exception.CodecException;
import com.hypixel.hytale.codec.util.RawJsonReader;
import com.martelstudios.openquests.core.models.QuestCompletions;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.models.QuestTally;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenDialoguesQuestCooldownConditionTest {

    private static final Instant START = Instant.ofEpochSecond(1000);
    private static final Instant END = START.plusSeconds(300);
    private static final QuestTally NEVER = QuestTally.of(QuestCompletions.NONE, UUID.randomUUID(), List.of());
    private static final QuestTally DONE = QuestTally.of(QuestCompletions.NONE.record(QuestState.SUCCESSFUL, START, END), UUID.randomUUID(), List.of());

    /**
     * Decodes without running validators: the asset id check needs the quest store loaded.
     */
    private static OpenDialoguesQuestCooldownCondition decode(String json) throws IOException {
        try (RawJsonReader reader = RawJsonReader.fromBuffer(json.toCharArray())) {
            return OpenDialoguesQuestCooldownCondition.CODEC.decodeJson(reader, new ExtraInfo());
        }
    }

    @Test
    void counts_from_the_end_by_default() throws IOException {
        var condition = decode("{\"QuestAssetId\":\"ElderPotion\",\"Seconds\":60}");

        assertFalse(condition.isActive(NEVER, END));
        assertTrue(condition.isActive(DONE, END.plusSeconds(59)));
        assertFalse(condition.isActive(DONE, END.plusSeconds(60)));
    }

    @Test
    void counts_from_the_start_when_asked() throws IOException {
        var condition = decode("{\"QuestAssetId\":\"ElderPotion\",\"Since\":\"Start\",\"Seconds\":600}");

        assertTrue(condition.isActive(DONE, START.plusSeconds(599)));
        assertFalse(condition.isActive(DONE, START.plusSeconds(600)));
    }

    @Test
    void non_positive_duration_rejected() {
        assertThrows(CodecException.class, () -> decode("{\"QuestAssetId\":\"ElderPotion\",\"Seconds\":0}"));
    }
}
