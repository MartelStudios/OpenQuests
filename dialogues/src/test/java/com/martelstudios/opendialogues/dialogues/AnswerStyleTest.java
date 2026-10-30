package com.martelstudios.opendialogues.dialogues;

import com.hypixel.hytale.protocol.Color;
import com.martelstudios.opendialogues.CodecJson;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AnswerStyleTest {

    private static OpenDialoguesResponse answer(String json) {
        return DialogueFixtures.dialogue("Styled", "{\"Responses\":[" + json + "]}").getResponses()[0];
    }

    private static void assertRgb(int red, int green, int blue, Color color) {
        assertEquals(red, Byte.toUnsignedInt(color.red));
        assertEquals(green, Byte.toUnsignedInt(color.green));
        assertEquals(blue, Byte.toUnsignedInt(color.blue));
    }

    @Test
    void decodesAnswerColor() {
        assertRgb(0x2a, 0x6a, 0x3a, answer("{\"Text\":\"a\",\"Color\":\"#2a6a3a\"}").getColor());
    }

    @Test
    void rejectsMissingIcon() {
        assertNull(CodecJson.decode(OpenDialogueAsset.BODY_CODEC, "{\"Responses\":[{\"Text\":\"a\",\"Icon\":\"Icons/Missing.png\"}]}"));
    }

    @Test
    void acceptsShortAndRgbForms() {
        assertRgb(255, 255, 255, answer("{\"Text\":\"a\",\"Color\":\"#fff\"}").getColor());
        assertRgb(255, 255, 255, answer("{\"Text\":\"a\",\"Color\":\"rgb(255,255,255)\"}").getColor());
    }

    @Test
    void defaultsToNoStyle() {
        OpenDialoguesResponse response = answer("{\"Text\":\"a\"}");
        assertNull(response.getColor());
        assertNull(response.getIcon());
        assertNull(DialogueFixtures.dialogue("Plain", "{\"Responses\":[]}").getColor());
    }

    @Test
    void decodesDialogueColor() {
        assertRgb(0xff, 0xcc, 0x00, DialogueFixtures.dialogue("Tinted", "{\"Color\":\"#ffcc00\"}").getColor());
    }
}
