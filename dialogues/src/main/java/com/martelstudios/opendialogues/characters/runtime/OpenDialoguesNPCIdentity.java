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
                                                                                   .append(new KeyedCodec<>("Draws", new ArrayCodec<>(Codec.STRING, String[]::new)), (identity, draws) -> identity.draws.addAll(List.of(draws)), identity -> identity.draws.toArray(String[]::new))
                                                                                   .add()
                                                                                   .build();

    @Nullable
    private String character;

    @Nullable
    private String name;

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
     * @return the translation key of the name drawn, {@code null} for a nameless character.
     */
    @Nullable
    public String getName() {
        return name;
    }

    public void setName(@Nullable String name) {
        this.name = name;
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

    @Nonnull
    @Override
    public Component<EntityStore> clone() {
        OpenDialoguesNPCIdentity clone = new OpenDialoguesNPCIdentity();
        clone.character = character;
        clone.name = name;
        clone.draws.addAll(draws);
        return clone;
    }
}
