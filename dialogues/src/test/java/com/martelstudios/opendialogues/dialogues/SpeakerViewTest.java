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

    private static final DialogueSource MIRA = new DialogueSource(UUID.randomUUID(), "Herbalist", "npc.mira", "Lent");

    private final OpenDialogueSpeakerAsset villager = speaker("{ \"Name\": \"npc.villager\", \"Epithet\": \"npc.kweebec\", \"Portrait\": \"Icons/Villager.png\", \"AllowNPCOverride\": true }");

    @Test
    void speakerWithoutOverrideIgnoresNpc() {
        OpenDialogueSpeakerAsset guard = speaker("{ \"Name\": \"npc.guard\" }");

        assertEquals(new SpeakerView("npc.guard", null, null), SpeakerView.of(guard, MIRA, lent("{}")));
    }

    @Test
    void overrideTakesNpcName() {
        assertEquals(new SpeakerView("npc.mira", "npc.kweebec", "Icons/Villager.png"), SpeakerView.of(villager, MIRA, lent(null)));
    }

    @Test
    void overrideFallsBackFieldByField() {
        SpeakerView view = SpeakerView.of(villager, MIRA, lent("{ \"Portrait\": \"Icons/Mira.png\" }"));

        assertEquals(new SpeakerView("npc.mira", "npc.kweebec", "Icons/Mira.png"), view);
    }

    @Test
    void overrideWithoutSourceKeepsSpeaker() {
        assertEquals(new SpeakerView("npc.villager", "npc.kweebec", "Icons/Villager.png"), SpeakerView.of(villager, null, lent(null)));
    }

    @Test
    void overrideWithEmptyNameKeepsSpeakerName() {
        DialogueSource nameless = new DialogueSource(UUID.randomUUID(), "Herbalist", null, "Lent");

        assertEquals("npc.villager", SpeakerView.of(villager, nameless, lent(null)).name());
    }

    @Test
    void sourceWithoutLentSpeakerKeepsOwnFields() {
        DialogueSource plain = new DialogueSource(UUID.randomUUID(), "Herbalist", "npc.mira", null);

        assertEquals(new SpeakerView("npc.mira", "npc.kweebec", "Icons/Villager.png"), SpeakerView.of(villager, plain, lent("{ \"Portrait\": \"Icons/Mira.png\" }")));
    }

    @Nonnull
    private static OpenDialogueSpeakerAsset speaker(@Nonnull String json) {
        return CodecJson.decodeUnvalidated(OpenDialogueSpeakerAsset.CODEC, json);
    }

    /**
     * @param speaker the speaker lending its fields, {@code null} for one no pack declares.
     * @return a lookup giving the speaker the source lends, by id.
     */
    @Nonnull
    private static Function<String, OpenDialogueSpeakerAsset> lent(String speaker) {
        Map<String, OpenDialogueSpeakerAsset> speakers = speaker == null ? Map.of() : Map.of("Lent", speaker(speaker));
        return speakers::get;
    }
}
