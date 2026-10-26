package com.martelstudios.openquests.extension.quests.item;

import com.martelstudios.openquests.core.persistence.CodecJson;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

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
}
