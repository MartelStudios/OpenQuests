package com.martelstudios.openquests.extension.quests.reachvalue;

import com.martelstudios.openquests.core.persistence.CodecJson;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReachValueQuestAssetTest {

    @BeforeAll
    static void registerSources() {
        ReachValueFeature.registerSources();
    }

    @Test
    void aGameFlagSourceReachesLevelOneByDefault() {
        ReachValueQuestAsset asset = decode("\"Source\": { \"Type\": \"GameFlag\", \"Flag\": \"goblin_lab_power\" }");

        assertNotNull(asset);
        assertEquals(1, asset.getTargetQuantity());
        assertFalse(asset.isRelative());
        assertEquals("goblin_lab_power", assertInstanceOf(GameFlagSource.class, asset.getSource()).getFlag());
    }

    @Test
    void aMemoriesSourceReadsItsMeasure() {
        ReachValueQuestAsset asset = decode("\"Source\": { \"Type\": \"Memories\", \"Measure\": \"Found\" }, \"TargetQuantity\": 5, \"Relative\": true");

        assertNotNull(asset);
        assertEquals(5, asset.getTargetQuantity());
        assertTrue(asset.isRelative());
        assertEquals(MemoriesSource.Measure.FOUND, assertInstanceOf(MemoriesSource.class, asset.getSource()).getMeasure());
    }

    @Test
    void aSourceIsRequired() {
        assertNull(decode("\"TargetQuantity\": 1"));
    }

    @Test
    void aGameFlagSourceNeedsItsFlag() {
        assertNull(decode("\"Source\": { \"Type\": \"GameFlag\" }"));
    }

    private static ReachValueQuestAsset decode(String fields) {
        return CodecJson.decode(ReachValueQuestAsset.CODEC, "{ " + fields + " }", "quest");
    }
}
