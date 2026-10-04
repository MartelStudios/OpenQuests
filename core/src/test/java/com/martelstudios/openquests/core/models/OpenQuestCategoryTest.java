package com.martelstudios.openquests.core.models;

import com.hypixel.hytale.protocol.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenQuestCategoryTest {

    @Test
    void aCategoryIsDrawnInTheColoursItNames() {
        OpenQuestCategory category = new OpenQuestCategory();
        category.backgroundColor = new Color((byte) 0x5b, (byte) 0x3f, (byte) 0x8c);
        category.textColor = new Color((byte) 0xf1, (byte) 0xea, (byte) 0xfb);

        assertTrue("#5b3f8c".equalsIgnoreCase(category.getBackgroundColor()), category.getBackgroundColor());
        assertTrue("#f1eafb".equalsIgnoreCase(category.getTextColor()), category.getTextColor());
    }

    @Test
    void aCategoryNamingNoColourTakesTheJournals() {
        OpenQuestCategory category = new OpenQuestCategory();

        assertEquals("#2c3a52", category.getBackgroundColor());
        assertEquals("#d9e1ee", category.getTextColor());
    }
}
