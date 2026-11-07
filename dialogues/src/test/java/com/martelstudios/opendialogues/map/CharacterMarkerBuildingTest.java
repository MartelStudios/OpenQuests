package com.martelstudios.opendialogues.map;

import com.hypixel.hytale.protocol.packets.worldmap.MapMarker;
import com.martelstudios.opendialogues.characters.OpenDialogueCharacterAsset;
import com.martelstudios.opendialogues.characters.runtime.CharacterPositions;
import com.martelstudios.opendialogues.characters.runtime.CharacterPositionsAccess;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.martelstudios.opendialogues.characters.CharacterFixtures.character;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class CharacterMarkerBuildingTest {

    private static final UUID WORLD = UUID.randomUUID();
    private static final UUID SORREL = new UUID(0, 1);
    private static final UUID TANSY = new UUID(0, 2);
    private static final UUID STRANGER = new UUID(0, 3);
    private static final UUID MAGE = new UUID(0, 4);

    private final Map<String, OpenDialogueCharacterAsset> characters = Map.of(
        "farmer", character("farmer", "{}"),
        "stranger", character("stranger", "{}"),
        "mage", character("mage", "{\"MapVisibility\": \"Always\"}"));
    private final CharacterMarkerProvider provider = new CharacterMarkerProvider(new CharacterPositionsAccess(null, null), player -> null, characters::get);
    private final CharacterPositions positions = new CharacterPositions();

    @Test
    void meetingOneNpcRevealsEveryNpcOfTheCharacter() {
        positions.record("farmer", SORREL, new Vector3d(), "names.sorrel");
        positions.record("farmer", TANSY, new Vector3d(5, 0, 5), "names.tansy");
        positions.record("stranger", STRANGER, new Vector3d(), null);

        CharacterMarkerProvider.Built built = build("farmer", Map.of());

        assertEquals(List.of("OpenDialogues:" + SORREL, "OpenDialogues:" + TANSY), built.all().stream().map(marker -> marker.id).sorted().toList());
        assertFalse(built.undecided());
    }

    @Test
    void nameFallsBackToTheEpithet() {
        positions.record("farmer", SORREL, new Vector3d(), "names.sorrel");
        positions.record("farmer", TANSY, new Vector3d(), null);
        CharacterMarkerProvider.Look look = new CharacterMarkerProvider.Look(new CharacterMarkerRule.Shown("Coordinate.png", false), "epithets.farmer");

        CharacterMarkerProvider.Built built = CharacterMarkerProvider.join(seen(Map.of("farmer", look)), positions.version(), positions.view());

        assertEquals("names.sorrel", marker(built, SORREL).name.messageId);
        assertEquals("epithets.farmer", marker(built, TANSY).name.messageId);
    }

    @Test
    void compassHoldsOnlyMarkersAStatusPutsThere() {
        positions.record("farmer", SORREL, new Vector3d(), null);
        positions.record("mage", MAGE, new Vector3d(), null);

        CharacterMarkerProvider.Built built = build("farmer", Map.of("mage", List.of(new CharacterMapStatus("TurnIn.png", 1, true))));

        assertEquals(2, built.all().size());
        assertEquals(List.of("OpenDialogues:" + MAGE), built.compass().stream().map(marker -> marker.id).toList());
        assertNull(marker(built, SORREL).compassImage);
        assertEquals("TurnIn.png", marker(built, MAGE).compassImage);
    }

    @Test
    void aCharacterNotJudgedYetAsksForARecompute() {
        positions.record("farmer", SORREL, new Vector3d(), null);
        CharacterMarkerProvider.Seen seen = seen(Map.of());

        positions.record("mage", MAGE, new Vector3d(), null);

        assertEquals(true, CharacterMarkerProvider.join(seen, positions.version(), positions.view()).undecided());
    }

    private CharacterMarkerProvider.Built build(String met, Map<String, List<CharacterMapStatus>> statuses) {
        return CharacterMarkerProvider.join(seen(provider.looks(positions.view().keySet(), met::equals, statuses)), positions.version(), positions.view());
    }

    private CharacterMarkerProvider.Seen seen(Map<String, CharacterMarkerProvider.Look> shown) {
        return new CharacterMarkerProvider.Seen(WORLD, positions, positions.view().keySet(), shown);
    }

    private static MapMarker marker(CharacterMarkerProvider.Built built, UUID npc) {
        return built.all().stream().filter(marker -> marker.id.equals("OpenDialogues:" + npc)).findFirst().orElseThrow();
    }
}
