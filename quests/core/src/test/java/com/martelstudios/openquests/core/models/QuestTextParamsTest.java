package com.martelstudios.openquests.core.models;

import com.hypixel.hytale.server.core.Message;
import com.martelstudios.openquests.core.TestQuestAsset;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuestTextParamsTest {

    @Test
    void textTagsBecomeParams() {
        Map<String, String[]> tags = Map.of("Text.giver", new String[] {"farmers.someone"}, "Other", new String[] {"x"});

        assertEquals(Map.of("giver", "farmers.someone"), QuestTextParams.of(List.of("Text.giver", "Other"), tags::get));
    }

    @Test
    void emptyOrMissingValuesAreSkipped() {
        Map<String, String[]> tags = new HashMap<>();
        tags.put("Text.giver", new String[0]);
        tags.put("Text.blank", new String[] {""});

        assertEquals(Map.of(), QuestTextParams.of(List.of("Text.giver", "Text.place", "Text.blank", "Text."), tags::get));
    }

    @Test
    void paramsAreTranslatedIntoTheMessage() {
        Message message = QuestTextParams.apply(Message.translation("quest.title"), Map.of("giver", "farmers.someone"));

        assertEquals("farmers.someone", message.getFormattedMessage().messageParams.get("giver").messageId);
    }

    @Test
    void aTagWrittenOnTheQuestFillsItsTitle() {
        AbstractQuestProgression<?> quest = new TestQuestAsset().create();
        quest.apply(new AbstractQuestProgression.PutTag("Text.giver", new String[] {"farmers.mira"}));

        assertEquals("farmers.mira", quest.getTitle().getFormattedMessage().messageParams.get("giver").messageId);
        assertEquals("farmers.mira", quest.getDescription().getFormattedMessage().messageParams.get("giver").messageId);
    }
}
