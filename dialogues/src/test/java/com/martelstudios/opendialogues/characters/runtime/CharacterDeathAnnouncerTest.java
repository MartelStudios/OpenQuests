package com.martelstudios.opendialogues.characters.runtime;

import com.hypixel.hytale.protocol.FormattedMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CharacterDeathAnnouncerTest {

    @Test
    void theDrawnNameIsTranslatedNotShownAsAKey() {
        FormattedMessage message = CharacterDeathAnnouncer.message("names.sorrel", "farmer.epithet").getFormattedMessage();

        assertEquals("opendialogues.character.death.named", message.messageId);
        assertEquals("names.sorrel", message.messageParams.get("name").messageId);
        assertEquals("farmer.epithet", message.messageParams.get("epithet").messageId);
    }

    @Test
    void theLineFitsWhatTheNpcHas() {
        assertEquals("opendialogues.character.death.name", CharacterDeathAnnouncer.message("names.sorrel", null).getFormattedMessage().messageId);
        assertEquals("opendialogues.character.death.epithet", CharacterDeathAnnouncer.message(null, "farmer.epithet").getFormattedMessage().messageId);
        assertNull(CharacterDeathAnnouncer.message(null, null));
    }
}
