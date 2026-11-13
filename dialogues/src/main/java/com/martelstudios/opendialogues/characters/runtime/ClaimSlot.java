package com.martelstudios.opendialogues.characters.runtime;

import com.martelstudios.opendialogues.characters.CharacterScope;
import com.martelstudios.opendialogues.characters.OpenDialogueCharacterAsset;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;
import java.util.function.Function;

/**
 * What one claim holds: a unique character, in one world for a world scoped one.
 *
 * @param world the config UUID of the world, {@code null} for a character scoped to the universe.
 */
public record ClaimSlot(@Nonnull String characterId, @Nullable UUID world) {

    /**
     * @param characters finds a loaded character by id.
     * @param world      the config UUID of the world the claim would be made in.
     * @return the slot, {@code null} for an unknown character or one any number of NPCs may carry.
     */
    @Nullable
    public static ClaimSlot of(@Nonnull String characterId, @Nonnull Function<String, OpenDialogueCharacterAsset> characters, @Nonnull UUID world) {
        OpenDialogueCharacterAsset character = characters.apply(characterId);
        if (character == null || !character.getUniqueness().claims()) return null;
        return new ClaimSlot(characterId, character.getScope() == CharacterScope.Universe ? null : world);
    }
}
