package com.martelstudios.openquests.extension.quests.enterworld;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import com.martelstudios.openquests.core.utils.WorldNamePattern;

/**
 * Enter a world whose name matches a regular expression.
 */
public class EnterWorldQuestAsset extends OpenQuestAsset {

    public static final BuilderCodec<EnterWorldQuestAsset> CODEC =
        BuilderCodec.builder(EnterWorldQuestAsset.class, EnterWorldQuestAsset::new, OpenQuestAsset.BASE_CODEC)
                    .append(new KeyedCodec<>("WorldNamePattern", WorldNamePattern.CODEC, true), (asset, pattern) -> asset.worldNamePattern = pattern, asset -> asset.worldNamePattern)
                    .addValidator(Validators.nonNull())
                    .add()
                    .build();

    protected WorldNamePattern worldNamePattern;

    private EnterWorldQuestAsset() {}

    @Override
    public AbstractQuestProgression<?> create() {
        return new EnterWorldQuestProgression().setAssetId(getId());
    }

    public WorldNamePattern getWorldNamePattern() {
        return worldNamePattern;
    }
}
