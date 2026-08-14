package com.martelstudios.hyquests.extension.quests.count;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.hyquests.core.assets.QuestAsset;

/**
 * Config shared by every quest whose completion is "reach a target count of something" —
 * mirrors Hytale's {@code CountObjectiveTaskAsset}. Concrete subtypes only need to add what
 * they're counting (an item id, a block tag, ...); the target {@link #getCount() quantity}
 * itself lives here once.
 */
public abstract class CountQuestAsset extends QuestAsset {

    public static final BuilderCodec<CountQuestAsset> BASE_CODEC = BuilderCodec.abstractBuilder(CountQuestAsset.class, QuestAsset.BASE_CODEC)
                                                                                .append(new KeyedCodec<>("Count", Codec.INTEGER), (asset, quantity) -> asset.count = quantity, asset -> Integer.valueOf(asset.count))
                                                                                .add()
                                                                                .build();

    protected int count;

    public int getCount() {
        return count;
    }
}
