package com.martelstudios.opendialogues.dialogues;

import com.martelstudios.opendialogues.CodecJson;
import com.martelstudios.opendialogues.sessions.DialogueSource;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpeakerViewTest {

    private static final DialogueSource MIRA = new DialogueSource(UUID.randomUUID(), "Herbalist", "npc.mira");

    private final OpenDialogueSpeakerAsset villager = speaker("{ \"Name\": \"npc.villager\", \"Epithet\": \"npc.kweebec\", \"Portrait\": \"Icons/Villager.png\", \"AllowNPCOverride\": true }");

    @Test
    void speakerWithoutOverrideIgnoresNpc() {
        OpenDialogueSpeakerAsset guard = speaker("{ \"Name\": \"npc.guard\" }");

        assertEquals(new SpeakerView("npc.guard", null, null), SpeakerView.of(guard, MIRA, characters("{}")));
    }

    @Test
    void overrideTakesNpcName() {
        assertEquals(new SpeakerView("npc.mira", "npc.kweebec", "Icons/Villager.png"), SpeakerView.of(villager, MIRA, characters(null)));
    }

    @Test
    void overrideFallsBackFieldByField() {
        SpeakerView view = SpeakerView.of(villager, MIRA, characters("{ \"Portrait\": \"Icons/Mira.png\" }"));

        assertEquals(new SpeakerView("npc.mira", "npc.kweebec", "Icons/Mira.png"), view);
    }

    @Test
    void overrideWithoutSourceKeepsSpeaker() {
        assertEquals(new SpeakerView("npc.villager", "npc.kweebec", "Icons/Villager.png"), SpeakerView.of(villager, null, characters(null)));
    }

    @Test
    void overrideWithEmptyNameKeepsSpeakerName() {
        DialogueSource nameless = new DialogueSource(UUID.randomUUID(), "Herbalist", null);

        assertEquals("npc.villager", SpeakerView.of(villager, nameless, characters(null)).name());
    }

    @Nonnull
    private static OpenDialogueSpeakerAsset speaker(@Nonnull String json) {
        return CodecJson.decodeUnvalidated(OpenDialogueSpeakerAsset.CODEC, json);
    }

    /**
     * @param speaker the inline speaker lending its fields, {@code null} for a character without one.
     * @return a lookup giving the source's character the speaker its fields come from.
     */
    @Nonnull
    private static Function<String, OpenDialogueSpeakerAsset> characters(String speaker) {
        Map<String, OpenDialogueSpeakerAsset> speakers = speaker == null ? Map.of() : Map.of("Herbalist", speaker(speaker));
        return speakers::get;
    }
}
