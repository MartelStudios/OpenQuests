package com.martelstudios.openquests.extension.quests.quantity;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.core.models.QuestAsset;

/**
 * Config shared by every quest whose completion is "reach a target quantity of something".
 * Concrete subtypes only add what is being counted; the target quantity lives here once.
 */
public abstract class QuantityQuestAsset extends QuestAsset {

    public static final BuilderCodec<QuantityQuestAsset> BASE_CODEC = BuilderCodec.abstractBuilder(QuantityQuestAsset.class, QuestAsset.BASE_CODEC)
                                                                                  .append(new KeyedCodec<>("TargetQuantity", Codec.INTEGER), (asset, quantity) -> asset.targetQuantity = quantity, asset -> Integer.valueOf(asset.targetQuantity))
                                                                                  .add()
                                                                                  .build();

    protected int targetQuantity;

    public int getTargetQuantity() {
        return targetQuantity;
    }
}
