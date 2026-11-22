package com.martelstudios.opendialogues.characters;

import com.martelstudios.opendialogues.characters.filters.CharacterFilter;
import com.martelstudios.opendialogues.characters.filters.FilterContext;
import com.martelstudios.opendialogues.characters.filters.impl.OpenDialoguesNearCharacterFilter;
import com.martelstudios.opendialogues.characters.runtime.CharacterPositions;
import com.hypixel.hytale.component.Ref;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.martelstudios.opendialogues.characters.CharacterFixtures.filter;
import static com.martelstudios.opendialogues.characters.CharacterFixtures.kweebec;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FiltersTest {

    @Test
    void roleFilterHoldsForListedRole() {
        String json = "{ \"Type\": \"Role\", \"Roles\": [\"Kweebec_Seedling\"] }";

        assertTrue(filter(json).test(kweebec("Zone1")));
        assertFalse(filter(json).test(new FilterContext(null, new Ref<>(null), "default", "Trork", "Zone1", "Forest", Set.<String>of()::contains)));
    }

    @Test
    void groupFilterAsksMembership() {
        String json = "{ \"Type\": \"Group\", \"Groups\": [\"Kweebec_Civilian\"] }";

        assertTrue(filter(json).test(kweebec("Zone1", "Kweebec_Civilian")));
        assertFalse(filter(json).test(kweebec("Zone1", "Kweebec_Prisoner")));
    }

    @Test
    void zoneFilterFailsWithoutZone() {
        assertFalse(filter("{ \"Type\": \"Zone\", \"Zones\": [\"Zone1\"] }").test(kweebec(null)));
    }

    @Test
    void zoneFilterReadsNpcZone() {
        String json = "{ \"Type\": \"Zone\", \"Zones\": [\"Zone1\"] }";

        assertTrue(filter(json).test(kweebec("Zone1")));
        assertTrue(kweebec("Zone1").zone().equals("Zone1"));
    }

    @Test
    void biomeFilterHoldsForListedBiome() {
        assertTrue(filter("{ \"Type\": \"Biome\", \"Biomes\": [\"Forest\"] }").test(kweebec("Zone1")));
    }

    @Test
    void notInvertsFilter() {
        assertFalse(filter("{ \"Type\": \"World\", \"Worlds\": [\"default\"], \"Not\": true }").test(kweebec("Zone1")));
    }

    @Test
    void worldFilterMatchesExactNames() {
        assertTrue(filter("{ \"Type\": \"World\", \"Worlds\": [\"default\"] }").test(kweebec("Zone1")));
    }

    @Test
    void worldFilterMatchesTheWholeNameAsARegex() {
        CharacterFilter temple = filter("{ \"Type\": \"World\", \"Worlds\": [\"instance-(forgotten-temple-goblins|Forgotten_Temple-.*)\"] }");

        assertTrue(temple.test(inWorld("instance-forgotten-temple-goblins")));
        assertTrue(temple.test(inWorld("instance-Forgotten_Temple-1b2c")));
        assertFalse(temple.test(inWorld("my-instance-forgotten-temple-goblins")));
    }

    @Test
    void aMalformedWorldPatternIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> filter("{ \"Type\": \"World\", \"Worlds\": [\"(\"] }"));
    }

    @Test
    void nearCharacterHoldsWithinRadius() {
        Map<String, Collection<CharacterPositions.Spot>> spots = Map.of("Farmer", List.of(spot(10, 64, 0)));

        assertTrue(OpenDialoguesNearCharacterFilter.anyWithin(spots, List.of("Farmer"), new Vector3d(0, 64, 0), 16));
        assertFalse(OpenDialoguesNearCharacterFilter.anyWithin(spots, List.of("Farmer"), new Vector3d(0, 64, 0), 8));
    }

    @Test
    void nearCharacterIgnoresOtherCharacters() {
        Map<String, Collection<CharacterPositions.Spot>> spots = Map.of("Smith", List.of(spot(1, 64, 0)));

        assertFalse(OpenDialoguesNearCharacterFilter.anyWithin(spots, List.of("Farmer"), new Vector3d(0, 64, 0), 16));
    }

    @Test
    void notNearCharacterRefusesACandidateNextToOne() {
        CharacterFilter filter = filter("{ \"Type\": \"NearCharacter\", \"Characters\": [\"Farmer\"], \"Radius\": 16, \"Not\": true }");
        OpenDialoguesNearCharacterFilter near = (OpenDialoguesNearCharacterFilter) filter;

        assertFalse(near.holdsAt(Map.of("Farmer", List.of(spot(3, 64, 0))), new Vector3d(0, 64, 0)));
        assertTrue(near.holdsAt(Map.of("Farmer", List.of(spot(40, 64, 0))), new Vector3d(0, 64, 0)));
        // Without a store no farmer is known, so the candidate is not near one
        assertTrue(filter.test(kweebec("Zone1")));
    }

    @Test
    void theDrawReadsTheRawNeighbourhoodSoNotIsAppliedOnce() {
        OpenDialoguesNearCharacterFilter near = (OpenDialoguesNearCharacterFilter) filter("{ \"Type\": \"NearCharacter\", \"Characters\": [\"Farmer\"], \"Radius\": 16, \"Not\": true }");
        Map<String, Collection<CharacterPositions.Spot>> close = Map.of("Farmer", List.of(spot(3, 64, 0)));

        assertTrue(near.evaluateAt(close, new Vector3d(0, 64, 0)));
        assertFalse(near.evaluateAt(Map.of(), new Vector3d(0, 64, 0)));
    }

    @Test
    void nearCharacterRequiresAPositiveRadius() {
        assertThrows(IllegalArgumentException.class, () -> filter("{ \"Type\": \"NearCharacter\", \"Characters\": [\"Farmer\"], \"Radius\": 0 }"));
    }

    private static CharacterPositions.Spot spot(double x, double y, double z) {
        return new CharacterPositions.Spot(UUID.randomUUID(), new Vector3d(x, y, z), null);
    }

    private static FilterContext inWorld(String world) {
        return new FilterContext(null, new Ref<>(null), world, "Kweebec_Seedling", "Zone1", "Forest", Set.<String>of()::contains);
    }
}
