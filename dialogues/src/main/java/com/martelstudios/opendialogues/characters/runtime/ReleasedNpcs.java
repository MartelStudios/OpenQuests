package com.martelstudios.opendialogues.characters.runtime;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The NPCs that let their character go, saved with the universe so whatever was tied to one is
 * dropped even for a player offline when it happened. Every world thread reaches it, hence the lock.
 */
public class ReleasedNpcs {

    public static final BuilderCodec<ReleasedNpcs> CODEC = BuilderCodec.builder(ReleasedNpcs.class, ReleasedNpcs::new)
                                                                       .append(new KeyedCodec<>("Npcs", new MapCodec<>(Codec.LONG, HashMap<String, Long>::new)), ReleasedNpcs::putAll, ReleasedNpcs::snapshot)
                                                                       .documentation("Epoch milliseconds of each release by NPC UUID, initially empty.")
                                                                       .add()
                                                                       .documentation("Persists the NPCs that let their character go, so ties to them are dropped even for offline players.")
                                                                       .build();

    private final Map<String, Long> npcs = new HashMap<>();
    private volatile Runnable onChange = () -> {};

    /**
     * @param onChange run after every change, outside the lock, as {@link UniverseCharacters#setOnChange} explains.
     */
    public void setOnChange(@Nonnull Runnable onChange) {
        this.onChange = onChange;
    }

    /**
     * Remembers that an NPC let its character go at that moment.
     */
    public void add(@Nonnull UUID npc, @Nonnull Instant at) {
        synchronized (this) {
            npcs.put(npc.toString(), at.toEpochMilli());
        }
        onChange.run();
    }

    /**
     * Remembers that NPCs let their character go at that moment, saved once for them all.
     */
    public void addAll(@Nonnull Collection<UUID> released, @Nonnull Instant at) {
        synchronized (this) {
            for (UUID npc : released) npcs.put(npc.toString(), at.toEpochMilli());
        }
        onChange.run();
    }

    /**
     * Kept when the NPC carries a character again: what was tied to it before is told apart by date.
     *
     * @return when that NPC last let its character go, {@code null} for one not remembered.
     */
    @Nullable
    public synchronized Instant releasedAt(@Nonnull UUID npc) {
        Long millis = npcs.get(npc.toString());
        return millis == null ? null : Instant.ofEpochMilli(millis);
    }

    /**
     * A release in the same millisecond counts, as the NPC may have let go before the tie was written.
     *
     * @param since when the thing was tied to the NPC, {@code null} for unknown, taken as before any release.
     * @return whether a release at {@code releasedAt} came after the thing tied at {@code since}.
     */
    public static boolean releasedSince(@Nullable Instant releasedAt, @Nullable Instant since) {
        return releasedAt != null && (since == null || !releasedAt.isBefore(since));
    }

    /**
     * Forgets the releases older than the cutoff.
     */
    public void pruneBefore(@Nonnull Instant cutoff) {
        long limit = cutoff.toEpochMilli();
        boolean changed;
        synchronized (this) {
            changed = npcs.values().removeIf(at -> at < limit);
        }
        if (changed) onChange.run();
    }

    private synchronized void putAll(@Nonnull Map<String, Long> read) {
        npcs.putAll(read);
    }

    @Nonnull
    private synchronized Map<String, Long> snapshot() {
        return new HashMap<>(npcs);
    }
}
