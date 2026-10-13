package com.martelstudios.openquests.extension.hud;

import com.martelstudios.openquests.core.models.HudSettings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuestHudSettingsTest {

    @Test
    void theDefaultsAreReadBackFromTheirFileAndCompletedByTheBuiltInOnes(@TempDir Path dataDirectory) throws IOException {
        Files.writeString(dataDirectory.resolve("hud.json"), "{ \"Side\": \"Left\", \"Top\": 300 }");

        QuestHudSettings.load(dataDirectory);

        assertEquals(new HudSettings(true, HudSettings.Side.LEFT, 24, 300), QuestHudSettings.getDefaults());
    }
}
