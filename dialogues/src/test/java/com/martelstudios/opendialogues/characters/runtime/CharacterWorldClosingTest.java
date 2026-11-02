package com.martelstudios.opendialogues.characters.runtime;

import com.hypixel.hytale.event.EventBus;
import com.martelstudios.opendialogues.characters.CharacterFixtures;
import com.martelstudios.opendialogues.characters.OpenDialogueCharacterAsset;
import com.martelstudios.opendialogues.characters.events.CharacterEvent;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CharacterWorldClosingTest {

    static {
        CharacterFixtures.registerFilters();
    }

    private static final UUID WORLD_A = UUID.randomUUID();
    private static final UUID WORLD_B = UUID.randomUUID();

    private final Map<String, OpenDialogueCharacterAsset> characters = new HashMap<>();
    private final UniverseCharacters universe = new UniverseCharacters();
    private final EventBus bus = new EventBus(false);
    private final List<CharacterEvent.Lost> lost = new ArrayList<>();
    private final CharacterWorldClosing closing = new CharacterWorldClosing(() -> universe, characters::get, bus);

    @Test
    void closingFreesAliveAndLosesForever() {
        claim("Kid", "Alive", WORLD_A);
        claim("Hero", "Forever", WORLD_A);
        bus.registerGlobal(CharacterEvent.Lost.class, lost::add);

        closing.close(WORLD_A);

        assertTrue(universe.isAvailable("Kid", characters::get));
        assertTrue(universe.isLost("Hero"));
        assertEquals(1, lost.size());
        assertEquals("Hero", lost.getFirst().getCharacter());
        assertEquals(CharacterEvent.Lost.Cause.WorldClosed, lost.getFirst().getCause());
    }

    @Test
    void closingOtherWorldChangesNothing() {
        claim("Kid", "Alive", WORLD_A);
        claim("Hero", "Forever", WORLD_A);
        bus.registerGlobal(CharacterEvent.Lost.class, lost::add);

        closing.close(WORLD_B);

        assertFalse(universe.isAvailable("Kid", characters::get));
        assertFalse(universe.isLost("Hero"));
        assertTrue(lost.isEmpty());
    }

    private void claim(@Nonnull String id, @Nonnull String uniqueness, @Nonnull UUID world) {
        characters.put(id, CharacterFixtures.character(id, "{ \"Uniqueness\": \"" + uniqueness + "\", \"Scope\": \"Universe\" }"));
        universe.claimIfAvailable(id, UUID.randomUUID(), world, characters::get);
    }
}
