package com.martelstudios.openquests.dialogues.actions;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.codec.util.RawJsonReader;
import com.hypixel.hytale.logger.HytaleLogger;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import com.martelstudios.openquests.core.models.QuestState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class OpenQuestsSetQuestStateActionTest {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static OpenQuestsSetQuestStateAction decode(String json) {
        try (RawJsonReader reader = RawJsonReader.fromBuffer(json.toCharArray())) {
            ExtraInfo extraInfo = new AssetExtraInfo<>(new AssetExtraInfo.Data(OpenQuestAsset.class, "Test", null));
            OpenQuestsSetQuestStateAction action = OpenQuestsSetQuestStateAction.CODEC.decodeJson(reader, extraInfo);
            extraInfo.getValidationResults().logOrThrowValidatorExceptions(LOGGER);
            return action;
        } catch (Exception e) {
            return null;
        }
    }

    @Test
    void successful_decodes() {
        OpenQuestsSetQuestStateAction action = decode("{\"Quest\":\"ElderPotion\",\"State\":\"Successful\"}");

        assertEquals("ElderPotion", action.getQuest());
        assertEquals(QuestState.SUCCESSFUL, action.getState());
    }

    @Test
    void in_progress_is_rejected() {
        assertNull(decode("{\"Quest\":\"ElderPotion\",\"State\":\"InProgress\"}"));
    }

    @Test
    void state_is_required() {
        assertNull(decode("{\"Quest\":\"ElderPotion\"}"));
    }
}
