package com.martelstudios.opendialogues.characters.runtime;

import com.martelstudios.opendialogues.characters.CharacterScope;
import com.martelstudios.opendialogues.characters.OpenDialogueCharacterAsset;
import com.martelstudios.opendialogues.characters.ReleaseReason;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/**
 * Sends each character to the registry its scope names: the NPC's world, or the universe.
 *
 * @param world   the world's own unique characters.
 * @param worldId the config UUID of the NPC's world, kept on universe claims.
 * @param scopes  finds a character to read its scope, the last known one for a character just deleted.
 */
record ScopedClaims(@Nonnull UniqueCharacters world, @Nonnull UniverseCharacters universe, @Nonnull UUID worldId, @Nonnull Function<String, OpenDialogueCharacterAsset> scopes) implements CharacterClaims {

    @Override
    public boolean isAvailable(@Nonnull String characterId, @Nonnull Function<String, OpenDialogueCharacterAsset> characters) {
        return inUniverse(characterId) ? universe.isAvailable(characterId, characters) : world.isAvailable(characterId, characters);
    }

    @Override
    public boolean claimIfAvailable(@Nonnull String characterId, @Nonnull UUID npc, @Nonnull Function<String, OpenDialogueCharacterAsset> characters) {
        return inUniverse(characterId) ? universe.claimIfAvailable(characterId, npc, worldId, characters) : world.claimIfAvailable(characterId, npc, characters);
    }

    /**
     * Asks both registries: a character whose scope changed may still be claimed in the other one.
     */
    @Nonnull
    @Override
    public List<String> release(@Nonnull UUID npc, @Nonnull ReleaseReason reason, @Nonnull Function<String, OpenDialogueCharacterAsset> characters) {
        world.release(npc, reason, characters);
        return universe.release(npc, reason, characters);
    }

    private boolean inUniverse(@Nonnull String characterId) {
        OpenDialogueCharacterAsset character = scopes.apply(characterId);
        return character != null && character.getScope() == CharacterScope.Universe;
    }
}
