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
    void theWatchtowerQuestMatchesEveryTowerTopVolume() throws IOException {
        TriggerVolumeQuestAsset asset = decodeShipped("GoblinBreachWatchtower");

        assertNull(asset.findError());
        assertEquals(TriggerEventType.ENTER, asset.getTriggerEventType());

        // Volume names as the breach watchtower prefabs ship them
        for (String name : new String[] {"trash_pile_trigger", "trash_pile_trigger_b", "trash_pile_trigger4", "enkindled_trigger_3", "enkindled_trigger_d2", "void_tower_trigger"}) {
            assertTrue(asset.getVolumeNamePattern().matches(name), name);
        }
        assertFalse(asset.getVolumeNamePattern().matches("enkindled_trigger"));
        assertFalse(asset.getVolumeNamePattern().matches("my_trash_pile_trigger"));
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
