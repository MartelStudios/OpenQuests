package com.martelstudios.opendialogues.map;

import com.martelstudios.opendialogues.characters.MapVisibility;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CharacterMarkerRuleTest {

    private static final CharacterMapStatus TURN_IN = new CharacterMapStatus("TurnIn.png", 1, true);

    @Test
    void neverWinsOverAQuestTarget() {
        assertNull(CharacterMarkerRule.resolve(MapVisibility.Never, null, true, TURN_IN));
    }

    @Test
    void whenMetHiddenUntilMetOrTargeted() {
        assertNull(CharacterMarkerRule.resolve(MapVisibility.WhenMet, null, false, null));
        assertEquals(new CharacterMarkerRule.Shown("Coordinate.png", false), CharacterMarkerRule.resolve(MapVisibility.WhenMet, null, true, null));
        assertEquals(new CharacterMarkerRule.Shown("TurnIn.png", true), CharacterMarkerRule.resolve(MapVisibility.WhenMet, null, false, TURN_IN));
    }

    @Test
    void alwaysShownWithoutMeeting() {
        assertEquals(new CharacterMarkerRule.Shown("Own.png", false), CharacterMarkerRule.resolve(MapVisibility.Always, "Own.png", false, null));
    }

    @Test
    void iconOrder() {
        assertEquals("TurnIn.png", CharacterMarkerRule.resolve(MapVisibility.Always, "Own.png", true, TURN_IN).image());
        assertEquals("Own.png", CharacterMarkerRule.resolve(MapVisibility.Always, "Own.png", true, new CharacterMapStatus(null, 1, false)).image());
        assertEquals(CharacterMarkerRule.DEFAULT_ICON, CharacterMarkerRule.resolve(MapVisibility.Always, null, true, null).image());
    }

    @Test
    void mergeTakesHighestPriorityImageAndAnyCompass() {
        CharacterMapStatus merged = CharacterMarkerRule.merge(List.of(new CharacterMapStatus("Low.png", 1, true), new CharacterMapStatus("High.png", 5, false), new CharacterMapStatus(null, 9, false)));

        assertEquals("High.png", merged.image());
        assertEquals(true, merged.onCompass());
        assertNull(CharacterMarkerRule.merge(List.of()));
    }
}
