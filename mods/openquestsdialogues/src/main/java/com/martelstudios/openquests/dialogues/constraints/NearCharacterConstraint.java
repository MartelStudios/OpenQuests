package com.martelstudios.openquests.dialogues.constraints;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.martelstudios.opendialogues.OpenDialoguesPlugin;
import com.martelstudios.opendialogues.characters.runtime.CharacterPositions;
import com.martelstudios.openquests.core.constraints.QuestConstraint;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.targets.QuestTarget;
import com.martelstudios.openquests.dialogues.givers.TargetMode;
import org.joml.Vector3d;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Counts a player's progress only within a radius of an NPC carrying a character, any of them in
 * the player's world, or of the NPC that gave the quest: plant around the village farmer, hunt
 * beside the guard.
 */
public class NearCharacterConstraint extends QuestConstraint {

    public static final BuilderCodec<NearCharacterConstraint> CODEC = BuilderCodec.builder(NearCharacterConstraint.class, NearCharacterConstraint::new, QuestConstraint.BASE_CODEC)
                                                                                  .append(new KeyedCodec<>("Character", Codec.STRING), (constraint, character) -> constraint.character = character, constraint -> constraint.character)
                                                                                  .documentation("ID of the character whose NPCs progress happens around, required with Target Character.")
                                                                                  .add()
                                                                                  .append(new KeyedCodec<>("Target", new EnumCodec<>(TargetMode.class)), (constraint, target) -> constraint.target = target, constraint -> constraint.target)
                                                                                  .documentation(TargetMode.DOCUMENTATION)
                                                                                  .add()
                                                                                  .append(new KeyedCodec<>("Radius", Codec.DOUBLE, true), (constraint, radius) -> constraint.radius = radius, constraint -> Double.valueOf(constraint.radius))
                                                                                  .addValidator(Validators.greaterThan(0.0))
                                                                                  .add()
                                                                                  .validator((constraint, results) -> {
                                                                                      if (constraint.target == TargetMode.Character && constraint.character == null) results.fail("Character is required with Target Character");
                                                                                  })
                                                                                  .build();

    @Nullable
    protected String character;

    @Nonnull
    protected TargetMode target = TargetMode.Character;

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

        Map<String, Collection<CharacterPositions.Spot>> spots = OpenDialoguesPlugin.get().getCharacterPositions().view(world.getEntityStore().getStore());
        return within(target.resolve(character, quest), spots, player.getTransform().getPosition(), radius);
    }

    /**
     * @param spots the NPCs of each character in the world.
     * @return whether an NPC the target leads to lies within the radius of the point; none for no target.
     */
    public static boolean within(@Nullable QuestTarget target, @Nonnull Map<String, Collection<CharacterPositions.Spot>> spots, @Nonnull Vector3d point, double radius) {
        String characterId = TargetMode.characterOf(target);
        if (characterId == null) return false;

        Collection<CharacterPositions.Spot> candidates = spots.getOrDefault(characterId, List.of());
        if (target instanceof QuestTarget.Npc npc) candidates = candidates.stream().filter(spot -> spot.npc().equals(npc.npc())).toList();
        return anyWithin(candidates, point, radius);
    }

    /**
     * @return whether one of the spots lies within the radius of the point, its edge included.
     */
    public static boolean anyWithin(@Nonnull Collection<CharacterPositions.Spot> spots, @Nonnull Vector3d point, double radius) {
        double squared = radius * radius;
        return spots.stream().anyMatch(spot -> spot.position().distanceSquared(point) <= squared);
    }

    /**
     * @return the id of the fixed character whose NPCs progress happens around, {@code null} for one aimed at the giver.
     */
    @Nullable
    public String getCharacter() {
        return target == TargetMode.Character ? character : null;
    }

    /**
     * @return whom the constraint aims at.
     */
    @Nonnull
    public TargetMode getTargetMode() {
        return target;
    }

    /**
     * @return where that quest's progress must happen around, {@code null} for nowhere.
     */
    @Nullable
    public QuestTarget target(@Nonnull AbstractQuestProgression<?> quest) {
        return target.resolve(character, quest);
    }

    /**
     * @return how far from the NPC progress still counts, in blocks.
     */
    public double getRadius() {
        return radius;
    }
}
