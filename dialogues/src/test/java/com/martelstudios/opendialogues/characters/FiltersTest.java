package com.martelstudios.opendialogues.characters;

import com.martelstudios.opendialogues.characters.filters.FilterContext;
import com.hypixel.hytale.component.Ref;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.martelstudios.opendialogues.characters.CharacterFixtures.filter;
import static com.martelstudios.opendialogues.characters.CharacterFixtures.kweebec;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FiltersTest {

    @Test
    void roleFilterHoldsForListedRole() {
        String json = "{ \"Type\": \"Role\", \"Roles\": [\"Kweebec_Seedling\"] }";

        assertTrue(filter(json).test(kweebec("Zone1")));
        assertFalse(filter(json).test(new FilterContext(null, new Ref<>(null), null, "default", "Trork", "Zone1", "Forest", Set.<String>of()::contains)));
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
    void zoneFilterReadsPlayerZone() {
        String json = "{ \"Type\": \"Zone\", \"Zones\": [\"Zone1\"] }";

        assertTrue(filter(json).test(kweebec("Zone1")));
        assertTrue(kweebec("Zone1").playerZone().equals("Zone1"));
    }

    @Test
    void biomeFilterHoldsForListedBiome() {
        assertTrue(filter("{ \"Type\": \"Biome\", \"Biomes\": [\"Forest\"] }").test(kweebec("Zone1")));
    }

    @Test
    void notInvertsFilter() {
        assertFalse(filter("{ \"Type\": \"World\", \"Worlds\": [\"default\"], \"Not\": true }").test(kweebec("Zone1")));
    }
}
