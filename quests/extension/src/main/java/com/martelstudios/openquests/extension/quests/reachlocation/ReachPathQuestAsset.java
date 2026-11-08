package com.martelstudios.openquests.extension.quests.reachlocation;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.math.vector.Vector3dUtil;
import com.hypixel.hytale.server.core.asset.type.particle.config.ParticleSystem;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestAsset;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestProgression;
import org.joml.Vector3d;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Reach spots one after the other, such as the tops of pillars to jump across. Reaching a spot ahead
 * of the next one restarts the path, and so does landing outside every spot with
 * {@code ResetOnFall}. The count is the number of spots, whatever {@code TargetQuantity} says.
 */
public class ReachPathQuestAsset extends QuantityQuestAsset {

    public static final BuilderCodec<ReachPathQuestAsset> CODEC =
        BuilderCodec.builder(ReachPathQuestAsset.class, ReachPathQuestAsset::new, QuantityQuestAsset.BASE_CODEC)
                    .append(new KeyedCodec<>("Positions", new ArrayCodec<>(Vector3dUtil.CODEC, Vector3d[]::new)), (asset, positions) -> asset.positions = positions, asset -> asset.positions)
                    .addValidator(Validators.nonEmptyArray())
                    .add()
                    .append(new KeyedCodec<>("Radius", Codec.DOUBLE), (asset, radius) -> asset.radius = radius, asset -> Double.valueOf(asset.radius))
                    .add()
                    .append(new KeyedCodec<>("ResetOnFall", Codec.BOOLEAN), (asset, reset) -> asset.resetOnFall = reset, asset -> Boolean.valueOf(asset.resetOnFall))
                    .add()
                    .append(new KeyedCodec<>("Marker", Codec.STRING), (asset, marker) -> asset.marker = marker, asset -> asset.marker)
                    .addValidator(ParticleSystem.VALIDATOR_CACHE.getValidator())
                    .add()
                    .append(new KeyedCodec<>("FinalMarker", Codec.STRING), (asset, marker) -> asset.finalMarker = marker, asset -> asset.finalMarker)
                    .addValidator(ParticleSystem.VALIDATOR_CACHE.getValidator())
                    .add()
                    .append(new KeyedCodec<>("StepSound", Codec.STRING), (asset, sound) -> asset.stepSound = sound, asset -> asset.stepSound)
                    .addValidator(SoundEvent.VALIDATOR_CACHE.getValidator())
                    .add()
                    .append(new KeyedCodec<>("ResetSound", Codec.STRING), (asset, sound) -> asset.resetSound = sound, asset -> asset.resetSound)
                    .addValidator(SoundEvent.VALIDATOR_CACHE.getValidator())
                    .add()
                    .append(new KeyedCodec<>("ResetMessageKey", Codec.STRING), (asset, key) -> asset.resetMessageKey = key, asset -> asset.resetMessageKey)
                    .add()
                    .build();

    /**
     * Vertical reach of a spot: a player standing on it has their feet on its height.
     */
    static final double HEIGHT = 0.5;

    protected Vector3d[] positions = new Vector3d[0];
    protected double radius = 1.2;
    protected boolean resetOnFall;
    @Nullable
    protected String marker;
    @Nullable
    protected String finalMarker;
    @Nullable
    protected String stepSound;
    @Nullable
    protected String resetSound;
    @Nullable
    protected String resetMessageKey;

    ReachPathQuestAsset() {}

    @Override
    public QuantityQuestProgression.Builder<ReachPathQuestProgression, ?> builder() {
        return QuantityQuestProgression.Builder.of(new ReachPathQuestProgression(), getId());
    }

    /**
     * @return the number of spots, the only count a path has.
     */
    @Override
    public int getTargetQuantity() {
        return positions.length;
    }

    /**
     * @return the index of the spot this position stands on, or {@code -1} for none. Spots lie
     * further apart than twice the radius, so at most one matches.
     */
    public int spotAt(@Nonnull Vector3d position) {
        for (int i = 0; i < positions.length; i++) {
            Vector3d spot = positions[i];
            double dx = position.x - spot.x;
            double dz = position.z - spot.z;
            if (dx * dx + dz * dz <= radius * radius && Math.abs(position.y - spot.y) <= HEIGHT) return i;
        }
        return -1;
    }

    /**
     * @return the spot at that index, or {@code null} past the last one.
     */
    @Nullable
    public Vector3d getPosition(int index) {
        return index >= 0 && index < positions.length ? positions[index] : null;
    }

    /**
     * @return the marker for the spot at that index: the final one on the last spot when set.
     */
    @Nullable
    public String markerFor(int index) {
        return index == positions.length - 1 && finalMarker != null ? finalMarker : marker;
    }

    /**
     * @return whether landing outside every spot restarts the path.
     */
    public boolean isResetOnFall() {
        return resetOnFall;
    }

    /**
     * @return the sound played on each spot reached, its pitch rising along the path.
     */
    @Nullable
    public String getStepSound() {
        return stepSound;
    }

    /**
     * @return the sound played when the path restarts.
     */
    @Nullable
    public String getResetSound() {
        return resetSound;
    }

    /**
     * @return the chat line telling the player why the path restarted.
     */
    @Nullable
    public String getResetMessageKey() {
        return resetMessageKey;
    }
}
