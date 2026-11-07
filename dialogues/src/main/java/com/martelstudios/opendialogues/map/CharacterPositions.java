package com.martelstudios.opendialogues.map;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.component.Resource;
import com.hypixel.hytale.math.vector.Vector3dUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3d;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Where a world's NPCs carrying a character were last seen, saved with the world so an NPC in an
 * unloaded chunk keeps its marker. Written on the world thread, safe to read from any.
 */
public class CharacterPositions implements Resource<EntityStore> {

    /**
     * Moves shorter than this keep the stored spot, so an NPC idling in place rebuilds no marker.
     */
    private static final double MOVE_THRESHOLD = 1.0;

    /**
     * One NPC carrying a character.
     *
     * @param name the translation key of its drawn name, {@code null} for none.
     */
    public record Spot(@Nonnull UUID npc, @Nonnull Vector3d position, @Nullable String name) {}

    private record Placed(@Nonnull String character, @Nonnull Spot spot) {}

    /**
     * The saved form of a spot, as a codec needs a mutable class.
     */
    private static final class Saved {

        static final BuilderCodec<Saved> CODEC = BuilderCodec.builder(Saved.class, Saved::new)
                                                             .append(new KeyedCodec<>("Character", Codec.STRING), (saved, character) -> saved.character = character, saved -> saved.character)
                                                             .add()
                                                             .append(new KeyedCodec<>("Npc", Codec.STRING), (saved, npc) -> saved.npc = npc, saved -> saved.npc)
                                                             .add()
                                                             .append(new KeyedCodec<>("Position", Vector3dUtil.CODEC), (saved, position) -> saved.position = position, saved -> saved.position)
                                                             .add()
                                                             .append(new KeyedCodec<>("Name", Codec.STRING), (saved, name) -> saved.name = name, saved -> saved.name)
                                                             .add()
                                                             .build();

        String character;
        String npc;
        Vector3d position;
        String name;
    }

    public static final BuilderCodec<CharacterPositions> CODEC = BuilderCodec.builder(CharacterPositions.class, CharacterPositions::new)
                                                                             .append(new KeyedCodec<>("Npcs", new ArrayCodec<>(Saved.CODEC, Saved[]::new)), CharacterPositions::load, CharacterPositions::save)
                                                                             .add()
                                                                             .append(new KeyedCodec<>("UniverseGeneration", Codec.INTEGER), (positions, generation) -> positions.universeGeneration = generation, positions -> positions.universeGeneration)
                                                                             .add()
                                                                             .append(new KeyedCodec<>("WorldGeneration", Codec.INTEGER), (positions, generation) -> positions.worldGeneration = generation, positions -> positions.worldGeneration)
                                                                             .add()
                                                                             .build();

    private final Map<UUID, Placed> npcs = new ConcurrentHashMap<>();
    private volatile int universeGeneration;
    private volatile int worldGeneration;
    private volatile int version;

    /**
     * Records where an NPC carrying a character stands, replacing what it carried before.
     */
    public void record(@Nonnull String characterId, @Nonnull UUID npc, @Nonnull Vector3d position, @Nullable String name) {
        Placed known = npcs.get(npc);
        if (known != null && known.character.equals(characterId) && Objects.equals(known.spot.name(), name) && known.spot.position().distance(position) < MOVE_THRESHOLD) return;
        npcs.put(npc, new Placed(characterId, new Spot(npc, new Vector3d(position), name)));
        version++;
    }

    /**
     * Forgets an NPC that no longer carries a character.
     */
    public void forget(@Nonnull UUID npc) {
        if (npcs.remove(npc) != null) version++;
    }

    /**
     * Forgets every NPC, as a world reset makes them all draw again.
     */
    public void clear() {
        if (npcs.isEmpty()) return;
        npcs.clear();
        version++;
    }

    /**
     * Forgets every NPC once the universe or the world was reset since they were recorded: their
     * NPCs draw again.
     */
    public void catchUp(int universeGeneration, int worldGeneration) {
        if (universeGeneration <= this.universeGeneration && worldGeneration <= this.worldGeneration) return;
        this.universeGeneration = Math.max(this.universeGeneration, universeGeneration);
        this.worldGeneration = Math.max(this.worldGeneration, worldGeneration);
        clear();
    }

    /**
     * @return grows on every change, so a marker list built on an older one is stale.
     */
    public int version() {
        return version;
    }

    /**
     * @return the NPCs of each character, a copy.
     */
    @Nonnull
    public Map<String, Collection<Spot>> view() {
        Map<String, Collection<Spot>> view = new HashMap<>();
        npcs.values().forEach(placed -> view.computeIfAbsent(placed.character, id -> new ArrayList<>()).add(placed.spot));
        return view;
    }

    private void load(@Nonnull Saved[] saved) {
        for (Saved spot : saved) {
            if (spot.character == null || spot.npc == null || spot.position == null) continue;
            UUID npc = UUID.fromString(spot.npc);
            npcs.put(npc, new Placed(spot.character, new Spot(npc, spot.position, spot.name)));
        }
    }

    @Nonnull
    private Saved[] save() {
        return npcs.values().stream().map(placed -> {
            Saved saved = new Saved();
            saved.character = placed.character;
            saved.npc = placed.spot.npc().toString();
            saved.position = placed.spot.position();
            saved.name = placed.spot.name();
            return saved;
        }).toArray(Saved[]::new);
    }

    @Nonnull
    @Override
    public Resource<EntityStore> clone() {
        CharacterPositions clone = new CharacterPositions();
        clone.npcs.putAll(npcs);
        clone.universeGeneration = universeGeneration;
        clone.worldGeneration = worldGeneration;
        return clone;
    }
}
