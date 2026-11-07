package com.martelstudios.openquests.extension.quests.reachlocation;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.math.vector.Vector3dUtil;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.targets.QuestTarget;
import com.martelstudios.openquests.core.targets.TargetedQuest;
import org.joml.Vector3d;

import javax.annotation.Nullable;

/**
 * Reach a spot within a radius. Shown on the map, and on the compass once tracked, unless the asset
 * says {@code ShowOnMap: false}.
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
            .build();

    protected Vector3d position;
    protected double radius;
    protected boolean showOnMap = true;

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
}
