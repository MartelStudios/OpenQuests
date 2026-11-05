package com.martelstudios.openquests.extension.quests.item;

import com.hypixel.hytale.protocol.BenchRequirement;
import com.martelstudios.openquests.core.persistence.CodecJson;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestItemFilterTest {

    @Test
    void aFilterNamingNothingIsRefused() {
        assertNull(CodecJson.decode(QuestItemFilter.CODEC, "{}", "filter"));
    }

    @Test
    void aTagAloneIsEnough() {
        QuestItemFilter filter = CodecJson.decode(QuestItemFilter.CODEC, "{ \"BlockTag\": \"Wood\" }", "filter");

        assertNotNull(filter);
        assertNull(filter.getItemId());
        assertNull(filter.getResourceTypeId());
    }

    @Test
    void nothingMatchesAMissingId() {
        QuestItemFilter filter = CodecJson.decode(QuestItemFilter.CODEC, "{ \"BlockTag\": \"Wood\" }", "filter");

        assertNotNull(filter);
        assertFalse(filter.matches(null));
    }

    @Test
    void aCategoryIsReadOffTheBenchesOfARecipe() {
        QuestItemFilter filter = CodecJson.decode(QuestItemFilter.CODEC, "{ \"Category\": \"GoblinLab_Runes\" }", "filter");

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
