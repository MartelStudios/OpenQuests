package com.martelstudios.opendialogues.characters.runtime;

import com.martelstudios.opendialogues.CodecJson;
import com.martelstudios.opendialogues.characters.CharacterFixtures;
import com.martelstudios.opendialogues.characters.OpenDialogueCharacterAsset;
import com.martelstudios.opendialogues.characters.ReleaseReason;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UniverseCharactersTest {

    static {
        CharacterFixtures.registerFilters();
    }

    private static final UUID WORLD_A = UUID.randomUUID();

    private final Map<String, OpenDialogueCharacterAsset> characters = new HashMap<>();
    private final Function<String, OpenDialogueCharacterAsset> lookup = characters::get;
    private final UniverseCharacters universe = new UniverseCharacters();

    @Test
    void claimBlocksOtherWorlds() {
        character("Hero", "Alive");

        assertTrue(universe.claimIfAvailable("Hero", UUID.randomUUID(), WORLD_A, lookup));

        assertFalse(universe.isAvailable("Hero", lookup));
        assertFalse(universe.claimIfAvailable("Hero", UUID.randomUUID(), UUID.randomUUID(), lookup));
    }

    @Test
    void deathFreesAlive() {
        character("Hero", "Alive");
        UUID npc = UUID.randomUUID();
        universe.claimIfAvailable("Hero", npc, WORLD_A, lookup);

        assertEquals(List.of(), universe.release(npc, ReleaseReason.Death, lookup));
        assertTrue(universe.isAvailable("Hero", lookup));
        assertFalse(universe.isLost("Hero"));
    }

    @Test
    void deathConsumesForeverForGood() {
        character("Hero", "Forever");
        UUID npc = UUID.randomUUID();
        universe.claimIfAvailable("Hero", npc, WORLD_A, lookup);

        assertEquals(List.of("Hero"), universe.release(npc, ReleaseReason.Death, lookup));
        assertEquals(List.of(), universe.release(npc, ReleaseReason.Removed, lookup));
        assertTrue(universe.isLost("Hero"));
        assertFalse(universe.isAvailable("Hero", lookup));
    }

    @Test
    void removedAndRevokedFreeForever() {
        character("Hero", "Forever");
        UUID npc = UUID.randomUUID();
        universe.claimIfAvailable("Hero", npc, WORLD_A, lookup);
        universe.release(npc, ReleaseReason.Removed, lookup);
        assertTrue(universe.isAvailable("Hero", lookup));

        universe.claimIfAvailable("Hero", npc, WORLD_A, lookup);
        universe.release(npc, ReleaseReason.Revoked, lookup);
        assertTrue(universe.isAvailable("Hero", lookup));
        assertFalse(universe.isLost("Hero"));
    }

    @Test
    void unknownCharacterIsNotLost() {
        assertFalse(universe.isLost("Nobody"));
    }

    @Test
    void concurrentClaimsHaveOneWinner() throws Exception {
        character("Hero", "Alive");
        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Boolean>> claims = new java.util.ArrayList<>();
            for (int i = 0; i < 8; i++) {
                claims.add(pool.submit(() -> {
                    start.await();
                    return universe.claimIfAvailable("Hero", UUID.randomUUID(), UUID.randomUUID(), lookup);
                }));
            }
            start.countDown();
            int winners = 0;
            for (Future<Boolean> claim : claims) if (claim.get()) winners++;
            assertEquals(1, winners);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void codecKeepsClaims() {
        character("Hero", "Forever");
        character("Kid", "Alive");
        UUID hero = UUID.randomUUID();
        universe.claimIfAvailable("Hero", hero, WORLD_A, lookup);
        universe.claimIfAvailable("Kid", UUID.randomUUID(), WORLD_A, lookup);
        universe.release(hero, ReleaseReason.Death, lookup);

        UniverseCharacters read = CodecJson.roundTrip(UniverseCharacters.CODEC, universe);

        assertTrue(read.isLost("Hero"));
        assertFalse(read.isAvailable("Kid", lookup));
    }

    @Test
    void changesAreReported() {
        character("Hero", "Alive");
        int[] changes = {0};
        universe.setOnChange(() -> changes[0]++);

        universe.claimIfAvailable("Hero", UUID.randomUUID(), WORLD_A, lookup);
        universe.isAvailable("Hero", lookup);

        assertEquals(1, changes[0]);
    }

    private void character(@Nonnull String id, @Nonnull String uniqueness) {
        characters.put(id, CharacterFixtures.character(id, "{ \"Uniqueness\": \"" + uniqueness + "\", \"Scope\": \"Universe\" }"));
    }
}
