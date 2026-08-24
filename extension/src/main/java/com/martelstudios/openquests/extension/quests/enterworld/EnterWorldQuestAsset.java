package com.martelstudios.openquests.extension.quests.enterworld;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.QuestAsset;

/**
 * Enter a world whose name matches a regular expression.
 */
public class EnterWorldQuestAsset extends QuestAsset {

    public static final BuilderCodec<EnterWorldQuestAsset> CODEC =
        BuilderCodec.builder(EnterWorldQuestAsset.class, EnterWorldQuestAsset::new, QuestAsset.BASE_CODEC)
                    .append(new KeyedCodec<>("WorldNamePattern", Codec.STRING, true), (asset, pattern) -> asset.worldNamePattern = pattern, asset -> asset.worldNamePattern)
                    .addValidator(Validators.nonNull())
                    .add()
                    .build();

    protected String worldNamePattern;

    private EnterWorldQuestAsset() {}

    @Override
    public AbstractQuestProgression<?> create() {
        return new EnterWorldQuestProgression().setAssetId(getId());
    }

    public String getWorldNamePattern() {
        return worldNamePattern;
    }
}
