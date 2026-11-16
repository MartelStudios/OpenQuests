package com.martelstudios.openquests.dialogues.constraints;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.martelstudios.opendialogues.OpenDialoguesPlugin;
import com.martelstudios.opendialogues.characters.runtime.CharacterPositions;
import com.martelstudios.openquests.core.constraints.QuestConstraint;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import org.joml.Vector3d;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Counts a player's progress only within a radius of an NPC carrying a character, any of them in
 * the player's world: plant around the village farmer, hunt beside the guard.
 */
public class NearCharacterConstraint extends QuestConstraint {

    public static final BuilderCodec<NearCharacterConstraint> CODEC = BuilderCodec.builder(NearCharacterConstraint.class, NearCharacterConstraint::new, QuestConstraint.BASE_CODEC)
                                                                                  .append(new KeyedCodec<>("Character", Codec.STRING, true), (constraint, character) -> constraint.character = character, constraint -> constraint.character)
                                                                                  .addValidator(Validators.nonNull())
                                                                                  .add()
                                                                                  .append(new KeyedCodec<>("Radius", Codec.DOUBLE, true), (constraint, radius) -> constraint.radius = radius, constraint -> Double.valueOf(constraint.radius))
                                                                                  .addValidator(Validators.greaterThan(0.0))
                                                                                  .add()
                                                                                  .build();

    protected String character;

    protected double radius;

    protected NearCharacterConstraint() {}

    /**
     * Reads where the player last ticked and the NPCs last seen, which any thread may ask.
     */
    @Override
    public boolean allowsProgress(@Nonnull AbstractQuestProgression<?> quest, @Nonnull UUID actorId) {
        PlayerRef player = Universe.get().getPlayer(actorId);
        if (player == null) return false;

        UUID worldId = player.getWorldUuid();
        World world = worldId == null ? null : Universe.get().getWorld(worldId);
        if (world == null) return false;

        Collection<CharacterPositions.Spot> spots = OpenDialoguesPlugin.get().getCharacterPositions().view(world.getEntityStore().getStore()).getOrDefault(character, List.of());
        return anyWithin(spots, player.getTransform().getPosition(), radius);
    }

    /**
     * @return whether one of the spots lies within the radius of the point, its edge included.
     */
    public static boolean anyWithin(@Nonnull Collection<CharacterPositions.Spot> spots, @Nonnull Vector3d point, double radius) {
        double squared = radius * radius;
        return spots.stream().anyMatch(spot -> spot.position().distanceSquared(point) <= squared);
    }

    /**
     * @return the id of the character whose NPCs progress happens around.
     */
    @Nonnull
    public String getCharacter() {
        return character;
    }

    /**
     * @return how far from the NPC progress still counts, in blocks.
     */
    public double getRadius() {
        return radius;
    }
}
