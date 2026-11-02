package com.martelstudios.opendialogues.characters.runtime;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.martelstudios.opendialogues.characters.OpenDialogueCharacterAsset;
import com.martelstudios.opendialogues.characters.ReleaseReason;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * The unique characters scoped to the universe that some world already gave away, saved with the
 * universe. Every world thread reaches it, hence one lock around each call.
 */
public class UniverseCharacters {

    /**
     * The NPC carrying a character and its world, or the mark that the character is lost for good.
     */
    public static class Claim {

        public static final BuilderCodec<Claim> CODEC = BuilderCodec.builder(Claim.class, Claim::new)
                                                                    .append(new KeyedCodec<>("Npc", Codec.STRING), (claim, npc) -> claim.npc = npc, claim -> claim.npc)
                                                                    .add()
                                                                    .append(new KeyedCodec<>("World", Codec.STRING), (claim, world) -> claim.world = world, claim -> claim.world)
                                                                    .add()
                                                                    .append(new KeyedCodec<>("Consumed", Codec.BOOLEAN), (claim, consumed) -> claim.consumed = consumed, claim -> claim.consumed)
                                                                    .add()
                                                                    .build();

        private String npc;
        private String world;
        private boolean consumed;

        private Claim() {}

        private Claim(@Nonnull String npc, @Nonnull String world, boolean consumed) {
            this.npc = npc;
            this.world = world;
            this.consumed = consumed;
        }
    }

    public static final BuilderCodec<UniverseCharacters> CODEC = BuilderCodec.builder(UniverseCharacters.class, UniverseCharacters::new)
                                                                             .append(new KeyedCodec<>("Claims", new MapCodec<>(Claim.CODEC, HashMap<String, Claim>::new)), UniverseCharacters::putAll, UniverseCharacters::snapshot)
                                                                             .add()
                                                                             .build();

    private final Map<String, Claim> claims = new HashMap<>();
    private volatile Runnable onChange = () -> {};

    /**
     * @param onChange run after every change, under the lock, so the registry gets saved.
     */
    public void setOnChange(@Nonnull Runnable onChange) {
        this.onChange = onChange;
    }

    /**
     * @param characters finds a loaded character by id, {@code null} for one no pack declares.
     * @return whether a draw may still give that character in any world, never for an unknown one.
     */
    public synchronized boolean isAvailable(@Nonnull String characterId, @Nonnull Function<String, OpenDialogueCharacterAsset> characters) {
        OpenDialogueCharacterAsset character = characters.apply(characterId);
        return character != null && (!character.getUniqueness().claims() || !claims.containsKey(characterId));
    }

    /**
     * Checks and claims in one step, so two worlds drawing at once cannot both win.
     *
     * @param world the config UUID of the NPC's world, which a closing world settles by.
     * @return whether the character was free.
     */
    public synchronized boolean claimIfAvailable(@Nonnull String characterId, @Nonnull UUID npc, @Nonnull UUID world, @Nonnull Function<String, OpenDialogueCharacterAsset> characters) {
        if (!isAvailable(characterId, characters)) return false;
        if (characters.apply(characterId).getUniqueness().claims()) {
            claims.put(characterId, new Claim(npc.toString(), world.toString(), false));
            onChange.run();
        }
        return true;
    }

    /**
     * @return whether the character's last carrier was consumed, so no NPC will ever carry it again.
     */
    public synchronized boolean isLost(@Nonnull String characterId) {
        Claim claim = claims.get(characterId);
        return claim != null && claim.consumed;
    }

    /**
     * Frees what a released NPC carried, unless the reason consumes it; a consumed claim never changes again.
     *
     * @return the ids lost for good by this release.
     */
    @Nonnull
    public synchronized List<String> release(@Nonnull UUID npc, @Nonnull ReleaseReason reason, @Nonnull Function<String, OpenDialogueCharacterAsset> characters) {
        String id = npc.toString();
        List<String> lost = new ArrayList<>();
        boolean changed = claims.entrySet().removeIf(entry -> {
            if (entry.getValue().consumed || !entry.getValue().npc.equals(id)) return false;
            OpenDialogueCharacterAsset character = characters.apply(entry.getKey());
            if (character != null && character.getUniqueness().consumedBy(reason)) {
                entry.getValue().consumed = true;
                lost.add(entry.getKey());
                return false;
            }
            return true;
        });
        if (changed || !lost.isEmpty()) onChange.run();
        return lost;
    }

    /**
     * Settles the claims of a world closing for good: its NPCs leave without dying, yet a
     * {@code Forever} character goes with them, the author wanting {@code Alive} for one that returns.
     *
     * @param world the config UUID of the closing world.
     * @return the ids lost for good by this closing.
     */
    @Nonnull
    public synchronized List<String> closeWorld(@Nonnull UUID world, @Nonnull Function<String, OpenDialogueCharacterAsset> characters) {
        String id = world.toString();
        List<String> lost = new ArrayList<>();
        boolean changed = claims.entrySet().removeIf(entry -> {
            if (entry.getValue().consumed || !id.equals(entry.getValue().world)) return false;
            OpenDialogueCharacterAsset character = characters.apply(entry.getKey());
            if (character != null && character.getUniqueness().consumedBy(ReleaseReason.Death)) {
                entry.getValue().consumed = true;
                lost.add(entry.getKey());
                return false;
            }
            return true;
        });
        if (changed || !lost.isEmpty()) onChange.run();
        return lost;
    }

    private synchronized void putAll(@Nonnull Map<String, Claim> read) {
        claims.putAll(read);
    }

    /**
     * A copy for the codec, which may write the registry from a saving thread while worlds draw.
     */
    @Nonnull
    private synchronized Map<String, Claim> snapshot() {
        Map<String, Claim> copy = new HashMap<>();
        claims.forEach((characterId, claim) -> copy.put(characterId, new Claim(claim.npc, claim.world, claim.consumed)));
        return copy;
    }
}
