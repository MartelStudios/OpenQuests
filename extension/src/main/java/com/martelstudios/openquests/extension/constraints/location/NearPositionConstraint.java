package com.martelstudios.openquests.extension.constraints.location;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3dUtil;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.martelstudios.openquests.core.constraints.QuestConstraint;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import org.joml.Vector3d;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Counts a player's progress only within a radius of a position: gather the berries of this
 * orchard, hunt around this camp. A world pattern, when given, says which world the position is in.
 */
public class NearPositionConstraint extends QuestConstraint {

    public static final BuilderCodec<NearPositionConstraint> CODEC = BuilderCodec.builder(NearPositionConstraint.class, NearPositionConstraint::new, QuestConstraint.BASE_CODEC)
                                                                                 .append(new KeyedCodec<>("Position", Vector3dUtil.CODEC, true), (constraint, position) -> constraint.position = position, constraint -> constraint.position)
                                                                                 .addValidator(Validators.nonNull())
                                                                                 .add()
                                                                                 .append(new KeyedCodec<>("Radius", Codec.DOUBLE, true), (constraint, radius) -> constraint.radius = radius, constraint -> Double.valueOf(constraint.radius))
                                                                                 .addValidator(Validators.greaterThan(0.0))
                                                                                 .add()
                                                                                 .append(new KeyedCodec<>("WorldNamePattern", Codec.STRING), (constraint, pattern) -> constraint.worldNamePattern = WorldNamePattern.of(pattern), constraint -> constraint.worldNamePattern == null ? null : constraint.worldNamePattern.getSource())
                                                                                 .add()
                                                                                 .build();

    protected Vector3d position;

    protected double radius;

    /**
     * {@code null} for a position meant in whatever world the player is in.
     */
    @Nullable
    protected WorldNamePattern worldNamePattern;

    protected NearPositionConstraint() {}

    /**
     * Read off where the player last ticked, which any thread may ask.
     */
    @Override
    public boolean allowsProgress(@Nonnull AbstractQuestProgression<?> quest, @Nonnull UUID actorId) {
        PlayerRef player = Universe.get().getPlayer(actorId);
        if (player == null) return false;

        if (worldNamePattern != null && !worldNamePattern.matchesWorldOf(actorId)) return false;

        Transform transform = player.getTransform();
        return isWithin(transform.getPosition());
    }

    @Nullable
    @Override
    public String validate(@Nonnull OpenQuestAsset asset) {
        String error = worldNamePattern == null ? null : worldNamePattern.getError();
        return error == null ? null : "invalid WorldNamePattern: " + error;
    }

    /**
     * @return whether that point is inside the radius, its edge included.
     */
    public boolean isWithin(@Nonnull Vector3d point) {
        return point.distanceSquared(position) <= radius * radius;
    }

    /**
     * @return the centre of the area progress counts in.
     */
    @Nonnull
    public Vector3d getPosition() {
        return position;
    }

    /**
     * @return how far from the centre progress still counts, in blocks.
     */
    public double getRadius() {
        return radius;
    }
}
