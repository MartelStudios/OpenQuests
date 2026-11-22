package com.martelstudios.opendialogues.characters.filters.impl;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.martelstudios.opendialogues.OpenDialoguesPlugin;
import com.martelstudios.opendialogues.characters.filters.CharacterFilter;
import com.martelstudios.opendialogues.characters.filters.FilterContext;
import com.martelstudios.opendialogues.characters.runtime.CharacterPositions;
import org.joml.Vector3d;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Holds when an NPC carrying one of the listed characters stands within the radius. Reads the
 * recorded spots, so an NPC in an unloaded chunk still counts where it was last seen.
 */
public class OpenDialoguesNearCharacterFilter extends CharacterFilter {

    public static final String TYPE = "NearCharacter";

    public static final BuilderCodec<OpenDialoguesNearCharacterFilter> CODEC = BuilderCodec.builder(OpenDialoguesNearCharacterFilter.class, OpenDialoguesNearCharacterFilter::new, CharacterFilter.BASE_CODEC)
                                                                                           .append(new KeyedCodec<>("Characters", new ArrayCodec<>(Codec.STRING, String[]::new), true), (filter, characters) -> filter.characters = characters, filter -> filter.characters)
                                                                                           .addValidator(Validators.nonNull())
                                                                                           .addValidator(Validators.nonEmptyArray())
                                                                                           .documentation("Character IDs whose claimed NPCs are looked for.")
                                                                                           .add()
                                                                                           .append(new KeyedCodec<>("Radius", Codec.DOUBLE, true), (filter, radius) -> filter.radius = radius, filter -> filter.radius)
                                                                                           .addValidator(Validators.nonNull())
                                                                                           .addValidator(Validators.greaterThan(0.0))
                                                                                           .documentation("Distance in blocks from the candidate NPC.")
                                                                                           .add()
                                                                                           .build();

    protected String[] characters = new String[0];
    protected double radius;

    protected OpenDialoguesNearCharacterFilter() {}

    /**
     * A draw without a store or an NPC without position knows no neighbour, so the filter fails.
     */
    @Override
    protected boolean evaluate(@Nonnull FilterContext context) {
        OpenDialoguesPlugin plugin = OpenDialoguesPlugin.get();
        if (context.accessor() == null || plugin == null) return false;
        TransformComponent transform = context.accessor().getComponent(context.npc(), TransformComponent.getComponentType());
        if (transform == null) return false;
        return holdsAt(plugin.getCharacterPositions().view(context.accessor()), transform.getPosition());
    }

    /**
     * @return whether the filter holds, {@code Not} applied, for a candidate standing at that point.
     */
    public boolean holdsAt(@Nonnull Map<String, Collection<CharacterPositions.Spot>> spots, @Nonnull Vector3d point) {
        return anyWithin(spots, List.of(characters), point, radius) != not;
    }

    /**
     * @return whether one of those characters has a spot within the radius of the point.
     */
    public static boolean anyWithin(@Nonnull Map<String, Collection<CharacterPositions.Spot>> spots, @Nonnull Collection<String> characters, @Nonnull Vector3d point, double radius) {
        double squared = radius * radius;
        for (String character : characters) {
            for (CharacterPositions.Spot spot : spots.getOrDefault(character, List.of())) {
                if (spot.position().distanceSquared(point) <= squared) return true;
            }
        }
        return false;
    }
}
