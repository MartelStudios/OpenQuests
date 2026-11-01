package com.martelstudios.opendialogues.characters;

import com.martelstudios.opendialogues.CodecJson;
import com.martelstudios.opendialogues.characters.runtime.OpenDialoguesNPCIdentity;
import com.martelstudios.opendialogues.characters.runtime.UniqueCharacters;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdentityTest {

    private static final UUID NPC = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final Map<String, OpenDialogueCharacterAsset> characters = Map.of(
        "Mira", character("Alive"),
        "Oak", character("Forever"),
        "Farmer", character("None"));

    @Test
    void identitySurvivesSave() {
        OpenDialoguesNPCIdentity identity = new OpenDialoguesNPCIdentity();
        identity.setCharacter("Mira");
        identity.setName("npc.mira");
        identity.recordDraw("Village");

        OpenDialoguesNPCIdentity saved = CodecJson.roundTrip(OpenDialoguesNPCIdentity.CODEC, identity);

        assertEquals("Mira", saved.getCharacter());
        assertEquals("npc.mira", saved.getName());
        assertEquals(Set.of("Village"), saved.getDraws());
    }

    @Test
    void aliveIsFreeAgainAfterRelease() {
        UniqueCharacters unique = new UniqueCharacters();
        unique.claim("Mira", NPC);
        assertFalse(unique.isAvailable("Mira", characters::get));

        unique.release(NPC, ReleaseReason.Death, characters::get);

        assertTrue(unique.isAvailable("Mira", characters::get));
    }

    @Test
    void foreverStaysTakenAfterRelease() {
        UniqueCharacters unique = new UniqueCharacters();
        unique.claim("Oak", NPC);

        unique.release(NPC, ReleaseReason.Death, characters::get);

        assertFalse(unique.isAvailable("Oak", characters::get));
    }

    @Test
    void repeatableIsAlwaysAvailable() {
        UniqueCharacters unique = new UniqueCharacters();
        unique.claim("Farmer", NPC);

        assertTrue(unique.isAvailable("Farmer", characters::get));
    }

    @Test
    void claimsSurviveSave() {
        UniqueCharacters unique = new UniqueCharacters();
        unique.claim("Mira", NPC);

        UniqueCharacters saved = CodecJson.roundTrip(UniqueCharacters.CODEC, unique);

        assertFalse(saved.isAvailable("Mira", characters::get));
    }

    @Test
    void unknownCharacterIdIsPlainVillager() {
        OpenDialoguesNPCIdentity identity = new OpenDialoguesNPCIdentity();
        identity.setCharacter("Gone");
        UniqueCharacters unique = new UniqueCharacters();
        unique.claim("Gone", NPC);

        unique.release(NPC, ReleaseReason.Death, characters::get);

        assertEquals("Gone", CodecJson.roundTrip(OpenDialoguesNPCIdentity.CODEC, identity).getCharacter());
        assertNull(unique.claimant("Gone"));
    }

    @Test
    void foreverSurvivesRemovalAfterDeath() {
        UniqueCharacters unique = new UniqueCharacters();
        unique.claim("Oak", NPC);

        unique.release(NPC, ReleaseReason.Death, characters::get);
        unique.release(NPC, ReleaseReason.Removed, characters::get);

        assertFalse(unique.isAvailable("Oak", characters::get));
    }

    @Test
    void foreverIsFreeAgainAfterRemovalOrRevocation() {
        for (ReleaseReason reason : new ReleaseReason[] {ReleaseReason.Removed, ReleaseReason.Revoked}) {
            UniqueCharacters unique = new UniqueCharacters();
            unique.claim("Oak", NPC);

            unique.release(NPC, reason, characters::get);

            assertTrue(unique.isAvailable("Oak", characters::get), reason.name());
        }
    }

    @Nonnull
    private static OpenDialogueCharacterAsset character(@Nonnull String uniqueness) {
        return CodecJson.decode(OpenDialogueCharacterAsset.CODEC, "{ \"Uniqueness\": \"" + uniqueness + "\" }");
    }
}
