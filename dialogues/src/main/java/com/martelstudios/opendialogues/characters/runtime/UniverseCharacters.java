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
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

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
                                                                             .append(new KeyedCodec<>("Generation", Codec.INTEGER), (universe, generation) -> universe.generation = generation, UniverseCharacters::generation)
                                                                             .add()
                                                                             .append(new KeyedCodec<>("DeadGeneration", Codec.INTEGER), (universe, generation) -> universe.deadGeneration = generation, UniverseCharacters::deadGeneration)
                                                                             .add()
                                                                             .build();

    private final Map<String, Claim> claims = new HashMap<>();
    private volatile Runnable onChange = () -> {};
    private int generation;
    private int deadGeneration;

    /**
     * @return how many times every character was reset; worlds and NPCs behind it catch up when met.
     */
    public synchronized int generation() {
        return generation;
    }

    /**
     * @return how many times lost characters were brought back; worlds behind it drop their consumed claims.
     */
    public synchronized int deadGeneration() {
        return deadGeneration;
    }

    /**
     * Forgets every claim, consumed ones included, and starts a new generation.
     */
    public void reset() {
        synchronized (this) {
            claims.clear();
            generation++;
        }
        onChange.run();
    }

    /**
     * Brings back the characters lost for good, here and, once they catch up, in every world.
     *
     * @return the ids freed here, sorted.
     */
    @Nonnull
    public List<String> resetDead() {
        List<String> freed = new ArrayList<>();
        synchronized (this) {
            claims.entrySet().removeIf(entry -> entry.getValue().consumed && freed.add(entry.getKey()));
            deadGeneration++;
        }
        onChange.run();
        freed.sort(null);
        return freed;
    }

    /**
     * @return each claimed character with the UUID of its carrier, or {@code consumed}, sorted by id.
     */
    @Nonnull
    public synchronized Map<String, String> describe() {
        Map<String, String> described = new TreeMap<>();
        claims.forEach((characterId, claim) -> described.put(characterId, claim.consumed ? "consumed" : claim.npc));
        return described;
    }


    /**
     * @param onChange run after every change, outside the lock: saving encodes the registry under
     * the storage lock, so calling it under ours could deadlock with a scheduled save.
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
    public boolean claimIfAvailable(@Nonnull String characterId, @Nonnull UUID npc, @Nonnull UUID world, @Nonnull Function<String, OpenDialogueCharacterAsset> characters) {
        boolean claimed;
        synchronized (this) {
            if (!isAvailable(characterId, characters)) return false;
            claimed = characters.apply(characterId).getUniqueness().claims();
            if (claimed) claims.put(characterId, new Claim(npc.toString(), world.toString(), false));
        }
        if (claimed) onChange.run();
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
    public List<String> release(@Nonnull UUID npc, @Nonnull ReleaseReason reason, @Nonnull Function<String, OpenDialogueCharacterAsset> characters) {
        String id = npc.toString();
        return settle(claim -> claim.npc.equals(id), reason, characters);
    }

    /**
     * Settles the claims of a world closing for good: its NPCs leave without dying, yet a
     * {@code Forever} character goes with them, the author wanting {@code Alive} for one that returns.
     *
     * @param world the config UUID of the closing world.
     * @return the ids lost for good by this closing.
     */
    @Nonnull
    public List<String> closeWorld(@Nonnull UUID world, @Nonnull Function<String, OpenDialogueCharacterAsset> characters) {
        String id = world.toString();
        return settle(claim -> id.equals(claim.world), ReleaseReason.Death, characters);
    }

    /**
     * Frees the matching claims, or consumes those the reason consumes.
     *
     * @return the ids consumed.
     */
    @Nonnull
    private List<String> settle(@Nonnull Predicate<Claim> matches, @Nonnull ReleaseReason reason, @Nonnull Function<String, OpenDialogueCharacterAsset> characters) {
        List<String> lost = new ArrayList<>();
        boolean changed;
        synchronized (this) {
            changed = claims.entrySet().removeIf(entry -> {
                if (entry.getValue().consumed || !matches.test(entry.getValue())) return false;
                OpenDialogueCharacterAsset character = characters.apply(entry.getKey());
                if (character != null && character.getUniqueness().consumedBy(reason)) {
                    entry.getValue().consumed = true;
                    lost.add(entry.getKey());
                    return false;
                }
                return true;
            });
        }
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
