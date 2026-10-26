package com.martelstudios.openquests.extension.quests.triggervolume;

import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerEventType;
import com.martelstudios.openquests.core.persistence.CodecJson;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TriggerVolumeQuestAssetTest {

    @Test
    void eachTowerTopQuestMatchesOnlyItsOwnTopVolumes() throws IOException {
        // Volume names as the breach watchtower prefabs ship them; enkindled_trigger_d2 is a middle floor
        assertMatches("GoblinBreachMineTowerTop", new String[] {"trash_pile_trigger", "trash_pile_trigger_b", "trash_pile_trigger4", "trash_pile_trigger (2)"},
                      new String[] {"my_trash_pile_trigger", "enkindled_trigger_3"});
        assertMatches("GoblinBreachCryptTowerTop", new String[] {"enkindled_trigger_1", "enkindled_trigger_5"},
                      new String[] {"enkindled_trigger_d2", "enkindled_trigger", "void_tower_trigger1"});
        assertMatches("GoblinBreachVoidTowerTop", new String[] {"void_tower_trigger1", "void_tower_trigger4"},
                      new String[] {"void_tower_trigger", "trash_pile_trigger"});
    }

    private static void assertMatches(String questId, String[] matching, String[] other) throws IOException {
        TriggerVolumeQuestAsset asset = decodeShipped(questId);

        assertNull(asset.findError());
        assertEquals(TriggerEventType.ENTER, asset.getTriggerEventType());
        for (String name : matching) assertTrue(asset.getVolumeNamePattern().matches(name), name);
        for (String name : other) assertFalse(asset.getVolumeNamePattern().matches(name), name);
    }

    @Test
    void aMalformedPatternIsRefused() {
        TriggerVolumeQuestAsset asset = decode("\"VolumeNamePattern\": \"(\"");

        assertNotNull(asset.getVolumeNamePattern().getError());
        assertNotNull(asset.findError());
        assertFalse(asset.getVolumeNamePattern().matches("("));
    }

    @Test
    void theEventTypeDefaultsToEnter() {
        TriggerVolumeQuestAsset asset = decode("\"VolumeNamePattern\": \"top\"");

        assertEquals(TriggerEventType.ENTER, asset.getTriggerEventType());
        assertNull(asset.findError());
    }

    @Test
    void onlyEventsReachingTheBusAreAccepted() {
        assertNull(decode("\"VolumeNamePattern\": \"top\", \"TriggerEventType\": \"EXIT\"").findError());

        for (String type : new String[] {"TICK", "TAG_ADDED", "BLOCK_USED", "SIGNAL_RECEIVED", "ENTITY_DIED", "VOLUME_CREATE", "ENTRE"}) {
            assertNotNull(decode("\"VolumeNamePattern\": \"top\", \"TriggerEventType\": \"" + type + "\"").findError(), type);
        }
    }

    private static TriggerVolumeQuestAsset decode(String fields) {
        TriggerVolumeQuestAsset asset = CodecJson.decode(TriggerVolumeQuestAsset.CODEC, "{ \"TargetQuantity\": 1, " + fields + " }", "quest");
        assertNotNull(asset, fields);
        return asset;
    }

    /**
     * Decodes the shipped asset's own volume fields alone: the rest of it needs the asset stores. Read
     * off the sources, since example quests are kept out of the jar and its resources.
     */
    private static TriggerVolumeQuestAsset decodeShipped(String id) throws IOException {
        String json = Files.readString(Path.of("src/main/resources/Server/OpenQuests/Quests/" + id + ".json"));
        Matcher pattern = Pattern.compile("\"VolumeNamePattern\": \"[^\"]*\"").matcher(json);
        Matcher eventType = Pattern.compile("\"TriggerEventType\": \"[^\"]*\"").matcher(json);
        assertTrue(pattern.find(), id);
        assertTrue(eventType.find(), id);

        return decode(pattern.group() + ", " + eventType.group());
    }
}
