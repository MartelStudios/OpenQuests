package com.martelstudios.openquests.extension.quests.block;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestAsset;

/**
 * A quest counting blocks placed or broken. {@code AntiAbuse} keeps one block placed and broken over
 * and over from counting more than once.
 */
public abstract class BlockActionQuestAsset extends QuantityQuestAsset {

    public static final BuilderCodec<BlockActionQuestAsset> BASE_CODEC = BuilderCodec.abstractBuilder(BlockActionQuestAsset.class, QuantityQuestAsset.BASE_CODEC)
                                                                                     .append(new KeyedCodec<>("AntiAbuse", Codec.BOOLEAN), (asset, antiAbuse) -> asset.antiAbuse = antiAbuse, asset -> asset.antiAbuse)
                                                                                     .documentation("Prevents placed blocks from counting when broken and recovered player placements from counting when placed again, false by default.")
                                                                                     .add()
                                                                                     .documentation("Defines shared settings for quests that count block placement or breaking.")
                                                                                     .build();

    protected boolean antiAbuse;

    /**
     * @return whether breaking a block the quest's players placed, or placing again what came out of
     * one, is left out.
     */
    public boolean isAntiAbuse() {
        return antiAbuse;
    }
}
