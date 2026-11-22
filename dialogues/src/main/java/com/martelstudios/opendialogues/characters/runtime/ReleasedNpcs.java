package com.martelstudios.opendialogues.characters.runtime;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;

import javax.annotation.Nonnull;
import java.time.Instant;
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
     * Forgets an NPC that carries a character again, so its new story is not swept as the old one.
     */
    public void remove(@Nonnull UUID npc) {
        boolean changed;
        synchronized (this) {
            changed = npcs.remove(npc.toString()) != null;
        }
        if (changed) onChange.run();
    }

    /**
     * @return whether that NPC let its character go and is still remembered.
     */
    public synchronized boolean contains(@Nonnull UUID npc) {
        return npcs.containsKey(npc.toString());
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
