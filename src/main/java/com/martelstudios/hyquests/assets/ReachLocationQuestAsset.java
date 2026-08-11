package com.martelstudios.hyquests.assets;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.math.vector.Vector3dUtil;
import com.martelstudios.hyquests.models.AbstractQuest;
import com.martelstudios.hyquests.models.ReachLocationQuest;
import org.joml.Vector3d;

public class ReachLocationQuestAsset extends QuestAsset {

    public static final BuilderCodec<ReachLocationQuestAsset> CODEC =
        BuilderCodec.builder(ReachLocationQuestAsset.class, ReachLocationQuestAsset::new, QuestAsset.BASE_CODEC)
            .append(new KeyedCodec<>("Position", Vector3dUtil.CODEC), (asset, position) -> asset.position = position, asset -> asset.position)
            .add()
            .append(new KeyedCodec<>("Radius", Codec.DOUBLE), (asset, radius) -> asset.radius = radius, asset -> Double.valueOf(asset.radius))
            .add()
            .build();

    protected Vector3d position;
    protected double radius;

    private ReachLocationQuestAsset() {}

    @Override
    public AbstractQuest<?> create() {
        return new ReachLocationQuest().setQuestAssetId(getId());
    }

    public Vector3d getPosition() {
        return position;
    }

    public double getRadius() {
        return radius;
    }
}
