package com.martelstudios.opendialogues.displays;

import com.hypixel.hytale.protocol.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnswerTintsTest {

    private static Color rgb(int red, int green, int blue) {
        return new Color((byte) red, (byte) green, (byte) blue);
    }

    @Test
    void baseIsTheColour() {
        assertEquals("#2a6a3a", AnswerTints.base(rgb(0x2a, 0x6a, 0x3a)));
    }

    @Test
    void hoveredMovesQuarterTowardWhite() {
        assertEquals("#5f8f6b", AnswerTints.hovered(rgb(0x2a, 0x6a, 0x3a)));
        assertEquals("#ffffff", AnswerTints.hovered(rgb(0xff, 0xff, 0xff)));
    }

    @Test
    void pressedDarkensByQuarter() {
        assertEquals("#1f4f2b", AnswerTints.pressed(rgb(0x2a, 0x6a, 0x3a)));
        assertEquals("#000000", AnswerTints.pressed(rgb(0, 0, 0)));
    }

    @Test
    void bandLeansHalfwayFromGold() {
        assertEquals("#83401d", AnswerTints.band(rgb(0xd0, 0x40, 0x40)));
        assertEquals("#452d0a", AnswerTints.band(rgb(0, 0, 0)));
    }

    @Test
    void pressedBandIsBrighter() {
        assertEquals("#ae5526", AnswerTints.pressedBand(rgb(0xd0, 0x40, 0x40)));
    }
}
