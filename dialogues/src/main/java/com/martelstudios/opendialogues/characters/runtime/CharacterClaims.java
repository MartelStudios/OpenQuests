package com.martelstudios.opendialogues.characters.runtime;

import com.martelstudios.opendialogues.characters.OpenDialogueCharacterAsset;
import com.martelstudios.opendialogues.characters.ReleaseReason;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/**
 * Which unique characters are taken, as one NPC sees it: its world's, or the universe's for a
 * character scoped to the universe.
 */
interface CharacterClaims {

    /**
     * @param characters finds a loaded character by id, {@code null} for one no pack declares.
     * @return whether a draw may still give that character, never for an unknown one.
     */
    boolean isAvailable(@Nonnull String characterId, @Nonnull Function<String, OpenDialogueCharacterAsset> characters);

    /**
     * Claims a unique character for the NPC if nobody holds it, in one step so two threads cannot both win.
     *
     * @return whether the character was free; a repeatable one always is and claims nothing.
     */
    boolean claimIfAvailable(@Nonnull String characterId, @Nonnull UUID npc, @Nonnull Function<String, OpenDialogueCharacterAsset> characters);

    /**
     * Frees what a released NPC carried, unless the reason consumes it.
     *
     * @return the ids of universe characters this release lost for good.
     */
    @Nonnull
    List<String> release(@Nonnull UUID npc, @Nonnull ReleaseReason reason, @Nonnull Function<String, OpenDialogueCharacterAsset> characters);
}
