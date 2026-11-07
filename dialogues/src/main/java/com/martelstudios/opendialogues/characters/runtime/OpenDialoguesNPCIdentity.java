package com.martelstudios.opendialogues.characters.runtime;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Who an NPC became, saved on the NPC itself so every player meets the same one after any reload,
 * and gone with it. Keeps the distributions that already drew for it, a draw of nobody included.
 */
public class OpenDialoguesNPCIdentity implements Component<EntityStore> {

    public static final BuilderCodec<OpenDialoguesNPCIdentity> CODEC = BuilderCodec.builder(OpenDialoguesNPCIdentity.class, OpenDialoguesNPCIdentity::new)
                                                                                   .append(new KeyedCodec<>("Character", Codec.STRING), (identity, character) -> identity.character = character, identity -> identity.character)
                                                                                   .add()
                                                                                   .append(new KeyedCodec<>("Name", Codec.STRING), (identity, name) -> identity.name = name, identity -> identity.name)
                                                                                   .add()
                                                                                   .append(new KeyedCodec<>("Epithet", Codec.STRING), (identity, epithet) -> identity.epithet = epithet, identity -> identity.epithet)
                                                                                   .add()
                                                                                   .append(new KeyedCodec<>("Draws", new ArrayCodec<>(Codec.STRING, String[]::new)), (identity, draws) -> identity.draws.addAll(List.of(draws)), identity -> identity.draws.toArray(String[]::new))
                                                                                   .add()
                                                                                   .append(new KeyedCodec<>("UniverseGeneration", Codec.INTEGER), (identity, generation) -> identity.universeGeneration = generation, identity -> identity.universeGeneration)
                                                                                   .add()
                                                                                   .append(new KeyedCodec<>("WorldGeneration", Codec.INTEGER), (identity, generation) -> identity.worldGeneration = generation, identity -> identity.worldGeneration)
                                                                                   .add()
                                                                                   .build();

    private int universeGeneration;

    private int worldGeneration;

    @Nullable
    private String character;

    @Nullable
    private String name;

    @Nullable
    private String epithet;

    private final Set<String> draws = new LinkedHashSet<>();

    /**
     * @return the id of the character the NPC carries, {@code null} while it is nobody.
     */
    @Nullable
    public String getCharacter() {
        return character;
    }

    public void setCharacter(@Nullable String character) {
        this.character = character;
    }

    /**
     * @return the translation key of the name drawn, {@code null} when no trait drew one.
     */
    @Nullable
    public String getName() {
        return name;
    }

    public void setName(@Nullable String name) {
        this.name = name;
    }

    /**
     * @return the translation key of the epithet drawn, {@code null} for none.
     */
    @Nullable
    public String getEpithet() {
        return epithet;
    }

    public void setEpithet(@Nullable String epithet) {
        this.epithet = epithet;
    }

    /**
     * @return the ids of the distributions that already drew for the NPC.
     */
    @Nonnull
    public Set<String> getDraws() {
        return Collections.unmodifiableSet(draws);
    }

    /**
     * Marks a distribution as drawn for good, whatever it gave.
     */
    public void recordDraw(@Nonnull String distributionId) {
        draws.add(distributionId);
    }

    /**
     * Forgets the distributions that drew, so they draw again.
     */
    public void clearDraws() {
        draws.clear();
    }

    /**
     * @return the universe reset generation the NPC drew under.
     */
    public int getUniverseGeneration() {
        return universeGeneration;
    }

    /**
     * @return the world reset generation the NPC drew under.
     */
    public int getWorldGeneration() {
        return worldGeneration;
    }

    /**
     * Records the generations the NPC now drew under; an NPC behind either one is stale.
     */
    public void stamp(int universe, int world) {
        universeGeneration = universe;
        worldGeneration = world;
    }

    /**
     * @return whether the NPC carries nothing worth saving: no character, no drawn text, no draw.
     */
    public boolean isBlank() {
        return character == null && name == null && epithet == null && draws.isEmpty();
    }

    /**
     * @return whether the other identity saves the same, so an unchanged NPC is not written again.
     */
    public boolean sameAs(@Nonnull OpenDialoguesNPCIdentity other) {
        return Objects.equals(character, other.character) && Objects.equals(name, other.name) && Objects.equals(epithet, other.epithet) && draws.equals(other.draws)
               && universeGeneration == other.universeGeneration && worldGeneration == other.worldGeneration;
    }

    @Nonnull
    @Override
    public Component<EntityStore> clone() {
        OpenDialoguesNPCIdentity clone = new OpenDialoguesNPCIdentity();
        clone.character = character;
        clone.name = name;
        clone.epithet = epithet;
        clone.draws.addAll(draws);
        clone.universeGeneration = universeGeneration;
        clone.worldGeneration = worldGeneration;
        return clone;
    }
}
