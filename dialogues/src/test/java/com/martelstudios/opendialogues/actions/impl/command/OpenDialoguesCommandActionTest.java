package com.martelstudios.opendialogues.actions.impl.command;

import com.martelstudios.opendialogues.CodecJson;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenDialoguesCommandActionTest {

    @Test
    void replacesEveryPlaceholder() {
        assertEquals("/sudo Bob tp Bob", OpenDialoguesCommandAction.resolve("/sudo {player} tp {player}", "Bob"));
    }

    @Test
    void asPlayerDefaultsToFalse() {
        OpenDialoguesCommandAction action = CodecJson.decode(OpenDialoguesCommandAction.CODEC, "{ \"Command\": \"tp top\" }");

        assertNotNull(action);
        assertFalse(action.isAsPlayer());
    }

    @Test
    void asPlayerReadsTrue() {
        OpenDialoguesCommandAction action = CodecJson.decode(OpenDialoguesCommandAction.CODEC, "{ \"Command\": \"tp top\", \"AsPlayer\": true }");

        assertNotNull(action);
        assertTrue(action.isAsPlayer());
    }
}
