package com.martelstudios.opendialogues.characters.runtime;

import com.martelstudios.opendialogues.CodecJson;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CharacterSettingsTest {

    @Test
    void drawsDefaultsToTrue() {
        assertTrue(CodecJson.decode(CharacterSettings.CODEC, "{}").isDraws());
        assertFalse(CodecJson.decode(CharacterSettings.CODEC, "{ \"Draws\": false }").isDraws());
    }
}
