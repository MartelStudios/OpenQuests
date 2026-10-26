package com.martelstudios.openquests.core.models;

import com.martelstudios.openquests.core.persistence.CodecJson;
import com.martelstudios.openquests.core.persistence.PlayerQuestRecord;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class HudSettingsTest {

    @Test
    void eachFieldFallsThroughToTheLayerBelow() {
        HudSettings player = new HudSettings(false, null, null, null);
        HudSettings runtime = new HudSettings(null, HudSettings.Side.LEFT, null, null);

        assertEquals(new HudSettings(false, HudSettings.Side.LEFT, 24, 120), player.over(runtime).over(HudSettings.BUILT_IN));
    }

    @Test
    void aPlayerRecordKeepsWhatWasChosenAndNothingElse() {
        PlayerQuestRecord record = new PlayerQuestRecord();
        assertFalse(CodecJson.encode(PlayerQuestRecord.CODEC, record).contains("Hud"));

        record.setHud(new HudSettings(null, HudSettings.Side.LEFT, null, 200));
        String json = CodecJson.encode(PlayerQuestRecord.CODEC, record);

        assertEquals(record.getHud(), CodecJson.decode(PlayerQuestRecord.CODEC, json, "record").getHud());
    }
}
