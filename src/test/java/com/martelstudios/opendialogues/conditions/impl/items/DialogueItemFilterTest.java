package com.martelstudios.opendialogues.conditions.impl.items;

import com.martelstudios.opendialogues.CodecJson;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class DialogueItemFilterTest {

    @Test
    void aFilterNamingNothingIsRefused() {
        assertNull(CodecJson.decode(DialogueItemFilter.CODEC, "{}"));
    }

    @Test
    void aTagAloneIsEnough() {
        assertNotNull(CodecJson.decode(DialogueItemFilter.CODEC, "{ \"BlockTag\": \"Wood\" }"));
    }

    @Test
    void nothingMatchesAMissingId() {
        DialogueItemFilter filter = CodecJson.decode(DialogueItemFilter.CODEC, "{ \"BlockTag\": \"Wood\" }");

        assertNotNull(filter);
        assertFalse(filter.matches(null));
    }
}
