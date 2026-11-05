package com.martelstudios.opendialogues.conditions.impl.items;

import com.hypixel.hytale.protocol.BenchRequirement;
import com.martelstudios.opendialogues.CodecJson;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenDialoguesItemFilterTest {

    @Test
    void aFilterNamingNothingIsRefused() {
        assertNull(CodecJson.decode(OpenDialoguesItemFilter.CODEC, "{}"));
    }

    @Test
    void aTagAloneIsEnough() {
        assertNotNull(CodecJson.decode(OpenDialoguesItemFilter.CODEC, "{ \"BlockTag\": \"Wood\" }"));
    }

    @Test
    void nothingMatchesAMissingId() {
        OpenDialoguesItemFilter filter = CodecJson.decode(OpenDialoguesItemFilter.CODEC, "{ \"BlockTag\": \"Wood\" }");

        assertNotNull(filter);
        assertFalse(filter.matches(null));
    }

    @Test
    void aCategoryAloneIsEnough() {
        assertNotNull(CodecJson.decode(OpenDialoguesItemFilter.CODEC, "{ \"Category\": \"GoblinLab_Runes\" }"));
    }

    @Test
    void aCategoryIsReadOffTheBenchesOfARecipe() {
        OpenDialoguesItemFilter filter = CodecJson.decode(OpenDialoguesItemFilter.CODEC, "{ \"Category\": \"GoblinLab_Runes\" }");

        assertNotNull(filter);
        assertTrue(filter.benchesList(new BenchRequirement[] {bench("Other"), bench("GoblinLab_Runes")}));
        assertFalse(filter.benchesList(new BenchRequirement[] {bench("Other")}));
        assertFalse(filter.benchesList(new BenchRequirement[] {new BenchRequirement()}));
        assertFalse(filter.benchesList(null));
    }

    private static BenchRequirement bench(String category) {
        BenchRequirement bench = new BenchRequirement();
        bench.categories = new String[] {category};
        return bench;
    }
}
