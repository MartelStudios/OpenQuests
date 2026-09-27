package com.martelstudios.openquests.extension.quests.breakblock;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.extension.quests.item.QuestItemFilter;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestAsset;

/**
 * Break a number of blocks.
 */
public class BreakBlockQuestAsset extends QuantityQuestAsset {

    public static final BuilderCodec<BreakBlockQuestAsset> CODEC =
        BuilderCodec.builder(BreakBlockQuestAsset.class, BreakBlockQuestAsset::new, QuantityQuestAsset.BASE_CODEC)
                    .append(new KeyedCodec<>("BlockToBreak", QuestItemFilter.CODEC), (asset, block) -> asset.blockToBreak = block, asset -> asset.blockToBreak)
                    .add()
                    .build();

    protected QuestItemFilter blockToBreak;

    private BreakBlockQuestAsset() {}

    @Override
    public AbstractQuestProgression<?> create() {
        return new BreakBlockQuestProgression().setAssetId(getId());
    }

    public QuestItemFilter getBlockToBreak() {
        return blockToBreak;
    }
}
