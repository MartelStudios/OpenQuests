package com.martelstudios.openquests.extension.quests.item;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestAsset;

/**
 * A quest counting items as they are thrown or picked up, one at a time rather than read from the
 * inventory. {@code AntiAbuse} keeps one item thrown and picked up over and over from counting more
 * than once.
 */
public abstract class ItemExchangeQuestAsset extends QuantityQuestAsset {

    public static final BuilderCodec<ItemExchangeQuestAsset> BASE_CODEC = BuilderCodec.abstractBuilder(ItemExchangeQuestAsset.class, QuantityQuestAsset.BASE_CODEC)
                                                                                      .append(new KeyedCodec<>("AntiAbuse", Codec.BOOLEAN), (asset, antiAbuse) -> asset.antiAbuse = antiAbuse, asset -> asset.antiAbuse)
                                                                                      .add()
                                                                                      .build();

    protected boolean antiAbuse;

    /**
     * @return whether a pickup of the player's own throw, or a throw of what they just picked back up,
     * is left out.
     */
    public boolean isAntiAbuse() {
        return antiAbuse;
    }
}
