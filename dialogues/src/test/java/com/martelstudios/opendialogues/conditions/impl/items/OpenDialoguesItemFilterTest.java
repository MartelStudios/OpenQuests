package com.martelstudios.opendialogues.conditions.impl.items;

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
    void aCategoryMatchesItemsListingIt() {
        OpenDialoguesItemFilter filter = CodecJson.decode(OpenDialoguesItemFilter.CODEC, "{ \"Category\": \"GoblinLab_Runes\" }");

        assertNotNull(filter);
        assertTrue(filter.inCategory(new String[] {"Items.Ingredients", "GoblinLab_Runes"}));
        assertFalse(filter.inCategory(new String[] {"Items.Ingredients"}));
        assertFalse(filter.inCategory(null));
    }
}
