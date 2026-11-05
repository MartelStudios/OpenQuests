package com.martelstudios.opendialogues.characters.runtime;

import com.martelstudios.opendialogues.CodecJson;
import com.martelstudios.opendialogues.characters.CharacterFixtures;
import com.martelstudios.opendialogues.characters.OpenDialogueCharacterAsset;
import com.martelstudios.opendialogues.characters.ReleaseReason;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UniqueCharactersTest {

    static {
        CharacterFixtures.registerFilters();
    }

    private final Map<String, OpenDialogueCharacterAsset> characters = new HashMap<>();
    private final Function<String, OpenDialogueCharacterAsset> lookup = characters::get;
    private final UniqueCharacters unique = new UniqueCharacters();
    private final UUID oak = UUID.randomUUID();

    @BeforeEach
    void claimOakAndLoseMage() {
        characters.put("Oak", CharacterFixtures.character("Oak", "{ \"Uniqueness\": \"Alive\" }"));
        characters.put("Mage", CharacterFixtures.character("Mage", "{ \"Uniqueness\": \"Forever\" }"));
        UUID mage = UUID.randomUUID();
        unique.claimIfAvailable("Oak", oak, lookup);
        unique.claimIfAvailable("Mage", mage, lookup);
        unique.release(mage, ReleaseReason.Death, lookup);
    }

    @Test
    void catchUpOnUniverseResetEmptiesEverything() {
        assertEquals(List.of("Mage", "Oak"), unique.catchUp(1, 0));
        assertEquals(List.of(), unique.catchUp(1, 0));
    }

    @Test
    void catchUpOnDeadResetDropsConsumedOnly() {
        assertEquals(List.of("Mage"), unique.catchUp(0, 1));
        assertEquals(oak, unique.claimant("Oak"));
    }

    @Test
    void worldResetBumpsGeneration() {
        unique.reset();

        assertEquals(1, unique.generation());
        assertNull(unique.claimant("Oak"));
        assertEquals(Map.of(), unique.describe());
    }

    @Test
    void oldSaveReadsZero() {
        UniqueCharacters read = CodecJson.decode(UniqueCharacters.CODEC, "{ \"Claims\": {} }");

        assertEquals(0, read.generation());
        assertEquals(List.of(), read.catchUp(0, 0));
    }

    @Test
    void countersSurviveCodec() {
        unique.reset();
        unique.catchUp(2, 3);

        UniqueCharacters read = CodecJson.roundTrip(UniqueCharacters.CODEC, unique);

        assertEquals(1, read.generation());
        assertEquals(List.of(), read.catchUp(2, 3));
    }
}
