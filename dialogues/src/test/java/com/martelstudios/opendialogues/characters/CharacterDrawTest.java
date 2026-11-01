package com.martelstudios.opendialogues.characters;

import com.martelstudios.opendialogues.CodecJson;
import com.martelstudios.opendialogues.characters.runtime.CharacterAssigner;
import com.martelstudios.opendialogues.characters.runtime.CharacterDraw;
import com.martelstudios.opendialogues.characters.runtime.OpenDialoguesNPCIdentity;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.util.ArrayDeque;
import java.util.Deque;
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

        for (int i = 0; i < 100; i++) assertNull(CharacterDraw.draw(entries, id -> false, random));
    }

    @Test
    void drawFollowsWeights() {
        List<OpenDialogueDistributionAsset.Entry> entries = entries("{ \"Character\": \"Farmer\", \"Weight\": 3 }, { \"Character\": \"Mage\" }, { \"Weight\": 6 }");
        RandomGenerator rolls = rolls(0, 3, 4);

        assertEquals("Farmer", CharacterDraw.draw(entries, id -> true, rolls));
        assertEquals("Mage", CharacterDraw.draw(entries, id -> true, rolls));
        assertNull(CharacterDraw.draw(entries, id -> true, rolls));
    }

    @Test
    void takenCharacterLeavesItsWeightToTheOthers() {
        List<OpenDialogueDistributionAsset.Entry> entries = entries("{ \"Character\": \"Mira\", \"Weight\": 5 }, { \"Character\": \"Farmer\" }, {}");

        assertNull(CharacterDraw.draw(entries, "Farmer"::equals, rolls(1)));
    }

    @Test
    void drawWithNoEntryIsNothing() {
        assertNull(CharacterDraw.draw(List.of(), id -> true, rolls()));
    }

    @Test
    void assignRecordsNothingForever() {
        OpenDialogueDistributionAsset village = distribution("Village", "[]", "{}");
        OpenDialoguesNPCIdentity identity = new OpenDialoguesNPCIdentity();

        assertNull(CharacterAssigner.assign(identity, List.of(village), kweebec("Zone1"), id -> true, rolls(0)));
        assertNull(CharacterAssigner.assign(identity, List.of(village), kweebec("Zone1"), id -> true, rolls()));

        assertEquals(Set.of("Village"), identity.getDraws());
        assertNull(identity.getCharacter());
    }

    @Test
    void assignSkipsFailingFilters() {
        OpenDialogueDistributionAsset temple = distribution("Temple", "[ { \"Type\": \"World\", \"Worlds\": [\"temple\"] } ]", "{ \"Character\": \"Farmer\" }");
        OpenDialoguesNPCIdentity identity = new OpenDialoguesNPCIdentity();

        assertNull(CharacterAssigner.assign(identity, List.of(temple), kweebec("Zone1"), id -> true, rolls()));

        assertEquals(Set.of(), identity.getDraws());
    }

    @Test
    void assignStopsAtFirstCharacter() {
        OpenDialogueDistributionAsset b = distribution("B", "[]", "{ \"Character\": \"Mage\" }");
        OpenDialogueDistributionAsset a = distribution("A", "[]", "{ \"Character\": \"Farmer\" }");
        OpenDialoguesNPCIdentity identity = new OpenDialoguesNPCIdentity();

        assertEquals("Farmer", CharacterAssigner.assign(identity, List.of(b, a), kweebec("Zone1"), id -> true, rolls(0, 0)));

        assertEquals("Farmer", identity.getCharacter());
        assertEquals(Set.of("A"), identity.getDraws());
    }

    @Nonnull
    private static List<OpenDialogueDistributionAsset.Entry> entries(@Nonnull String entries) {
        return List.of(distribution("Test", "[]", entries).getEntries());
    }

    @Nonnull
    private static OpenDialogueDistributionAsset distribution(@Nonnull String id, @Nonnull String filters, @Nonnull String entries) {
        OpenDialogueDistributionAsset distribution = CodecJson.decode(OpenDialogueDistributionAsset.CODEC, "{ \"Filters\": " + filters + ", \"Characters\": [ " + entries + " ] }");
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
