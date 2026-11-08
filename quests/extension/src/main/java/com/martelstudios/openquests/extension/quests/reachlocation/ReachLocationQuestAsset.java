package com.martelstudios.openquests.extension.quests.reachlocation;

import com.hypixel.hytale.assetstore.map.AssetMapWithIndexes;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.math.vector.Vector3dUtil;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.asset.type.particle.config.ParticleSystem;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.targets.QuestTarget;
import com.martelstudios.openquests.core.targets.TargetedQuest;
import org.joml.Vector3d;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Reach a spot within a radius. Shown on the map, and on the compass once tracked, unless the asset
 * says {@code ShowOnMap: false}. {@code Marker} names a particle system shown on the spot, and
 * {@code Sound} a sound played, at {@code SoundPitch}, to whoever reaches it.
 */
public class ReachLocationQuestAsset extends OpenQuestAsset implements TargetedQuest {

    public static final BuilderCodec<ReachLocationQuestAsset> CODEC =
        BuilderCodec.builder(ReachLocationQuestAsset.class, ReachLocationQuestAsset::new, OpenQuestAsset.BASE_CODEC)
            .append(new KeyedCodec<>("Position", Vector3dUtil.CODEC), (asset, position) -> asset.position = position, asset -> asset.position)
            .add()
            .append(new KeyedCodec<>("Radius", Codec.DOUBLE), (asset, radius) -> asset.radius = radius, asset -> Double.valueOf(asset.radius))
            .add()
            .append(new KeyedCodec<>("ShowOnMap", Codec.BOOLEAN), (asset, show) -> asset.showOnMap = show, asset -> asset.showOnMap)
            .add()
            .append(new KeyedCodec<>("Marker", Codec.STRING), (asset, marker) -> asset.marker = marker, asset -> asset.marker)
            .addValidator(ParticleSystem.VALIDATOR_CACHE.getValidator())
            .add()
            .append(new KeyedCodec<>("Sound", Codec.STRING), (asset, sound) -> asset.sound = sound, asset -> asset.sound)
            .addValidator(SoundEvent.VALIDATOR_CACHE.getValidator())
            .add()
            .append(new KeyedCodec<>("SoundPitch", Codec.FLOAT), (asset, pitch) -> asset.soundPitch = pitch, asset -> Float.valueOf(asset.soundPitch))
            .add()
            .build();

    protected Vector3d position;
    protected double radius;
    protected boolean showOnMap = true;
    @Nullable
    protected String marker;
    @Nullable
    protected String sound;
    protected float soundPitch = 1;

    private ReachLocationQuestAsset() {}

    @Nullable
    @Override
    public QuestTarget target() {
        return showOnMap && position != null ? new QuestTarget.Location(new Vector3d(position)) : null;
    }

    @Override
    public AbstractQuestProgression.Builder<ReachLocationQuestProgression, ?> builder() {
        return AbstractQuestProgression.Builder.of(new ReachLocationQuestProgression(), getId());
    }

    public Vector3d getPosition() {
        return position;
    }

    public double getRadius() {
        return radius;
    }

    /**
     * @return whether a player standing there is within the radius of the spot.
     */
    public boolean contains(@Nonnull Vector3d at) {
        return position != null && at.distanceSquared(position) <= radius * radius;
    }

    /**
     * @return the particle system shown on the spot to its players while they run the quest, if any.
     */
    @Nullable
    public String getMarker() {
        return marker;
    }

    /**
     * Plays the spot's sound, if it has one, to the player who reached it.
     */
    public void playSound(@Nonnull PlayerRef player) {
        playSound(player, sound, soundPitch);
    }

    /**
     * Plays a sound event to one player alone, ignoring an unset or unknown one.
     */
    static void playSound(@Nonnull PlayerRef player, @Nullable String soundEventId, float pitch) {
        if (soundEventId == null) return;

        int index = SoundEvent.getAssetMap().getIndex(soundEventId);
        if (index != AssetMapWithIndexes.NOT_FOUND) SoundUtil.playSoundEvent2dToPlayer(player, index, SoundCategory.UI, 1, pitch);
    }
}
