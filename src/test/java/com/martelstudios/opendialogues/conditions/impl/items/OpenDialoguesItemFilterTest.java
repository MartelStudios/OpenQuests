package com.martelstudios.opendialogues.conditions.impl.items;

import com.martelstudios.opendialogues.CodecJson;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

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
}
