package com.martelstudios.opendialogues.characters;

import com.martelstudios.opendialogues.CodecJson;
import com.martelstudios.opendialogues.characters.runtime.CharacterAssigner;
import com.martelstudios.opendialogues.characters.runtime.CharacterDraw;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.random.RandomGenerator;

import static com.martelstudios.opendialogues.characters.CharacterFixtures.kweebec;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CharacterDrawTest {

    static {
        CharacterFixtures.registerFilters();
    }

    @Test
    void drawNeverPicksUnavailableCharacter() {
        List<OpenDialogueDistributionAsset.Entry> entries = entries("{ \"Character\": \"Mira\" }, {}");
        SplittableRandom random = new SplittableRandom(42);

        for (int i = 0; i < 100; i++) assertNull(CharacterDraw.draw(entries, id -> false, random).getCharacter());
    }

    @Test
    void drawFollowsWeights() {
        List<OpenDialogueDistributionAsset.Entry> entries = entries("{ \"Character\": \"Farmer\", \"Weight\": 3 }, { \"Character\": \"Mage\" }, { \"Weight\": 6 }");
        RandomGenerator rolls = rolls(0, 3, 4);

        assertEquals("Farmer", CharacterDraw.draw(entries, id -> true, rolls).getCharacter());
        assertEquals("Mage", CharacterDraw.draw(entries, id -> true, rolls).getCharacter());
        assertNull(CharacterDraw.draw(entries, id -> true, rolls).getCharacter());
    }

    @Test
    void takenCharacterLeavesItsWeightToTheOthers() {
        List<OpenDialogueDistributionAsset.Entry> entries = entries("{ \"Character\": \"Mira\", \"Weight\": 5 }, { \"Character\": \"Farmer\" }, {}");

        assertNull(CharacterDraw.draw(entries, "Farmer"::equals, rolls(1)).getCharacter());
    }

    @Test
    void drawWithNoEntryIsNothing() {
        assertNull(CharacterDraw.draw(List.of(), id -> true, rolls()));
    }

    @Test
    void assignRecordsNothingForever() {
        OpenDialogueDistributionAsset village = distribution("Village", "[]", "{}");

        CharacterAssigner.Draw draw = CharacterAssigner.assign(Set.of(), List.of(village), kweebec("Zone1"), id -> true, rolls(0));
        assertEquals(List.of("Village"), draw.drawn());
        assertNull(draw.character());

        assertEquals(List.of(), CharacterAssigner.assign(Set.of("Village"), List.of(village), kweebec("Zone1"), id -> true, rolls()).drawn());
    }

    @Test
    void assignSkipsFailingFilters() {
        OpenDialogueDistributionAsset temple = distribution("Temple", "[ { \"Type\": \"World\", \"Worlds\": [\"temple\"] } ]", "{ \"Character\": \"Farmer\" }");

        CharacterAssigner.Draw draw = CharacterAssigner.assign(Set.of(), List.of(temple), kweebec("Zone1"), id -> true, rolls());

        assertEquals(List.of(), draw.drawn());
        assertNull(draw.character());
    }

    @Test
    void assignStopsAtFirstCharacter() {
        OpenDialogueDistributionAsset b = distribution("B", "[]", "{ \"Character\": \"Mage\" }");
        OpenDialogueDistributionAsset a = distribution("A", "[]", "{ \"Character\": \"Farmer\" }");

        CharacterAssigner.Draw draw = CharacterAssigner.assign(Set.of(), List.of(b, a), kweebec("Zone1"), id -> true, rolls(0, 0));

        assertEquals("Farmer", draw.character());
        assertEquals(List.of("A"), draw.drawn());
    }

    @Test
    void assignOrdersByPriorityThenId() {
        OpenDialogueDistributionAsset b = distribution("B", "[]", "{ \"Character\": \"Mage\" }", 5);
        OpenDialogueDistributionAsset a = distribution("A", "[]", "{ \"Character\": \"Farmer\" }", 0);

        CharacterAssigner.Draw draw = CharacterAssigner.assign(Set.of(), List.of(a, b), kweebec("Zone1"), id -> true, rolls(0));

        assertEquals("Mage", draw.character());
        assertEquals(List.of("B"), draw.drawn());
    }

    @Test
    void assignLeavesIdentityUntouched() {
        OpenDialogueDistributionAsset a = distribution("A", "[]", "{}");
        OpenDialogueDistributionAsset b = distribution("B", "[]", "{ \"Character\": \"Farmer\" }");
        Set<String> already = new HashSet<>(Set.of("Old"));

        CharacterAssigner.Draw draw = CharacterAssigner.assign(already, List.of(a, b), kweebec("Zone1"), id -> true, rolls(0, 0));

        assertEquals(List.of("A", "B"), draw.drawn());
        assertEquals(Set.of("Old"), already);
    }

    @Test
    void assignRecordsNothingWhenNoEntryIsEligible() {
        OpenDialogueDistributionAsset village = distribution("Village", "[]", "{ \"Character\": \"Oak\" }");

        CharacterAssigner.Draw draw = CharacterAssigner.assign(Set.of(), List.of(village), kweebec("Zone1"), id -> false, rolls());

        assertEquals(List.of(), draw.drawn());
        assertNull(draw.character());
    }

    @Nonnull
    private static List<OpenDialogueDistributionAsset.Entry> entries(@Nonnull String entries) {
        return List.of(distribution("Test", "[]", entries).getEntries());
    }

    @Nonnull
    private static OpenDialogueDistributionAsset distribution(@Nonnull String id, @Nonnull String filters, @Nonnull String entries) {
        return distribution(id, filters, entries, 0);
    }

    @Nonnull
    private static OpenDialogueDistributionAsset distribution(@Nonnull String id, @Nonnull String filters, @Nonnull String entries, int priority) {
        OpenDialogueDistributionAsset distribution = CodecJson.decode(OpenDialogueDistributionAsset.CODEC, "{ \"Priority\": " + priority + ", \"Filters\": " + filters + ", \"Characters\": [ " + entries + " ] }");
        if (distribution == null) throw new IllegalArgumentException("Unreadable distribution " + entries);
        distribution.id = id;
        return distribution;
    }

    /**
     * Hands out the given rolls in order, so a test reads which entry each one lands on.
     */
    @Nonnull
    private static RandomGenerator rolls(int... values) {
        Deque<Integer> queue = new ArrayDeque<>();
        for (int value : values) queue.add(value);
        return new RandomGenerator() {
            @Override
            public long nextLong() {
                throw new AssertionError("Only bounded int rolls are expected");
            }

            @Override
            public int nextInt(int bound) {
                Integer roll = queue.poll();
                if (roll == null) throw new AssertionError("Random consulted once more than expected");
                return roll;
            }
        };
    }
}
