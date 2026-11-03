package com.martelstudios.openquests.extension.quests.gameflag;

import com.martelstudios.openquests.core.persistence.CodecJson;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameFlagQuestAssetTest {

    @Test
    void theLevelDefaultsToOne() {
        GameFlagQuestAsset asset = decode("\"Flag\": \"goblin_lab_power\"");

        assertNotNull(asset);
        assertTrue(asset.isReached(key -> key.equals("goblin_lab_power") ? 1 : 0));
        assertFalse(asset.isReached(key -> 0));
    }

    @Test
    void aHigherFlagStillCounts() {
        GameFlagQuestAsset asset = decode("\"Flag\": \"goblin_breach_duration\", \"Level\": 2");

        assertNotNull(asset);
        assertTrue(asset.isReached(key -> 3));
        assertFalse(asset.isReached(key -> 1));
    }

    @Test
    void aFlagIsRequired() {
        assertNull(decode("\"Level\": 1"));
    }

    private static GameFlagQuestAsset decode(String fields) {
        return CodecJson.decode(GameFlagQuestAsset.CODEC, "{ " + fields + " }", "quest");
    }
}
