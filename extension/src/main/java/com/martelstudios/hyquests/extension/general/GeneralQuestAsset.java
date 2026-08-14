package com.martelstudios.hyquests.extension.general;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.martelstudios.hyquests.core.assets.QuestAsset;

/**
 * Config for a composite quest: the ids of the {@link QuestAsset}s its children are created
 * from. Mirrors {@link com.martelstudios.hyquests.core.models.GeneralQuest}, which resolves
 * its own children by id rather than embedding them.
 */
public class GeneralQuestAsset extends QuestAsset {

    public static final BuilderCodec<GeneralQuestAsset> CODEC = BuilderCodec.builder(GeneralQuestAsset.class, GeneralQuestAsset::new, QuestAsset.BASE_CODEC)
                                                                            .append(new KeyedCodec<>("Assets", new ArrayCodec<>(Codec.STRING, String[]::new)), (asset, ids) -> asset.assetIds = ids, asset -> asset.assetIds)
                                                                            .add()
                                                                            .build();

    protected String[] assetIds = new String[0];

    private GeneralQuestAsset() {}

    @Override
    public GeneralQuest create() {
        return new GeneralQuest().setQuestAssetId(getId());
    }

    public String[] getAssetIds() {
        return assetIds;
    }
}
