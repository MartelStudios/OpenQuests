package com.martelstudios.opendialogues.dialogues;

import com.martelstudios.opendialogues.CodecJson;
import com.martelstudios.opendialogues.characters.OpenDialogueCharacterAsset;
import com.martelstudios.opendialogues.sessions.DialogueSource;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SpeakerViewTest {

    private static final DialogueSource SOURCE = new DialogueSource(UUID.randomUUID(), "Source", "n.src", "e.src");

    private final Function<String, OpenDialogueCharacterAsset> characters = Map.of(
        "Dialogue", character("{ \"Name\": \"n.dlg\", \"Epithet\": \"e.dlg\", \"Portrait\": \"p.dlg\" }"),
        "Source", character("{ \"Portrait\": \"p.src\" }"),
        "Bare", character("{}"))::get;

    @Test
    void useSourceTakesSourceIdentity() {
        assertEquals(new SpeakerView("n.src", "e.src", "p.src"), SpeakerView.of("Dialogue", true, SOURCE, characters));
    }

    @Test
    void useSourceFallsBackFieldByField() {
        DialogueSource bare = new DialogueSource(UUID.randomUUID(), "Bare", null, null);

        assertEquals(new SpeakerView("n.dlg", "e.dlg", "p.dlg"), SpeakerView.of("Dialogue", true, bare, characters));
    }

    @Test
    void useSourceWithoutSourceShowsCharacter() {
        assertEquals(new SpeakerView("n.dlg", "e.dlg", "p.dlg"), SpeakerView.of("Dialogue", true, null, characters));
    }

    @Test
    void useSourceWithoutSourceOrCharacterShowsNobody() {
        assertNull(SpeakerView.of(null, true, null, characters));
    }

    @Test
    void useSourceWithoutCharacterShowsSource() {
        assertEquals(new SpeakerView("n.src", "e.src", "p.src"), SpeakerView.of(null, true, SOURCE, characters));
    }

    @Test
    void fixedCharacterIgnoresSource() {
        assertEquals(new SpeakerView("n.dlg", "e.dlg", "p.dlg"), SpeakerView.of("Dialogue", false, SOURCE, characters));
    }

    @Test
    void unknownCharacterShowsNobody() {
        assertNull(SpeakerView.of("Missing", false, SOURCE, characters));
    }

    @Nonnull
    private static OpenDialogueCharacterAsset character(@Nonnull String json) {
        return CodecJson.decodeUnvalidated(OpenDialogueCharacterAsset.CODEC, json);
    }
}
