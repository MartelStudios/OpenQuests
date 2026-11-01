package com.martelstudios.opendialogues.characters.runtime;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.component.Resource;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.opendialogues.characters.OpenDialogueCharacterAsset;
import com.martelstudios.opendialogues.characters.ReleaseReason;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Which unique characters a world already gave away, saved with the world: an NPC in an unloaded
 * chunk still holds its character, so only its death frees it.
 */
public class UniqueCharacters implements Resource<EntityStore> {

    /**
     * The NPC carrying a character, or the mark that its last carrier died for good.
     */
    public static class Claim {

        public static final BuilderCodec<Claim> CODEC = BuilderCodec.builder(Claim.class, Claim::new)
                                                                    .append(new KeyedCodec<>("Npc", Codec.STRING), (claim, npc) -> claim.npc = npc, claim -> claim.npc)
                                                                    .add()
                                                                    .append(new KeyedCodec<>("Consumed", Codec.BOOLEAN), (claim, consumed) -> claim.consumed = consumed, claim -> claim.consumed)
                                                                    .add()
                                                                    .build();

        private String npc;
        private boolean consumed;

        private Claim() {}

        private Claim(@Nonnull String npc, boolean consumed) {
            this.npc = npc;
            this.consumed = consumed;
        }
    }

    public static final BuilderCodec<UniqueCharacters> CODEC = BuilderCodec.builder(UniqueCharacters.class, UniqueCharacters::new)
                                                                           .append(new KeyedCodec<>("Claims", new MapCodec<>(Claim.CODEC, HashMap<String, Claim>::new)), (unique, claims) -> unique.claims.putAll(claims), unique -> unique.claims)
                                                                           .add()
                                                                           .build();

    private final Map<String, Claim> claims = new HashMap<>();

    /**
     * @param characters finds a loaded character by id, {@code null} for one no pack declares.
     * @return whether a draw may still give that character in this world, never for an unknown one.
     */
    public boolean isAvailable(@Nonnull String characterId, @Nonnull Function<String, OpenDialogueCharacterAsset> characters) {
        OpenDialogueCharacterAsset character = characters.apply(characterId);
        return character != null && (!character.getUniqueness().claims() || !claims.containsKey(characterId));
    }

    /**
     * Records that an NPC now carries the character.
     */
    public void claim(@Nonnull String characterId, @Nonnull UUID npc) {
        claims.put(characterId, new Claim(npc.toString(), false));
    }

    /**
     * @return the NPC carrying the character, {@code null} when none does, its last carrier dead included.
     */
    @Nullable
    public UUID claimant(@Nonnull String characterId) {
        Claim claim = claims.get(characterId);
        return claim == null || claim.consumed ? null : UUID.fromString(claim.npc);
    }

    /**
     * Frees what a released NPC carried, unless the reason consumes it; a consumed claim never changes again.
     *
     * @param characters finds a loaded character by id, {@code null} for one no pack declares any more.
     */
    public void release(@Nonnull UUID npc, @Nonnull ReleaseReason reason, @Nonnull Function<String, OpenDialogueCharacterAsset> characters) {
        String id = npc.toString();
        claims.entrySet().removeIf(entry -> {
            if (entry.getValue().consumed || !entry.getValue().npc.equals(id)) return false;
            OpenDialogueCharacterAsset character = characters.apply(entry.getKey());
            if (character != null && character.getUniqueness().consumedBy(reason)) {
                entry.getValue().consumed = true;
                return false;
            }
            return true;
        });
    }

    @Nonnull
    @Override
    public Resource<EntityStore> clone() {
        UniqueCharacters clone = new UniqueCharacters();
        claims.forEach((characterId, claim) -> clone.claims.put(characterId, new Claim(claim.npc, claim.consumed)));
        return clone;
    }
}
