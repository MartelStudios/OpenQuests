package com.martelstudios.openquests.extension.quests.breakblock;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.extension.quests.block.BlockActionQuestAsset;
import com.martelstudios.openquests.extension.quests.item.QuestItemFilter;

/**
 * Break a number of blocks.
 */
public class BreakBlockQuestAsset extends BlockActionQuestAsset {

    public static final BuilderCodec<BreakBlockQuestAsset> CODEC =
        BuilderCodec.builder(BreakBlockQuestAsset.class, BreakBlockQuestAsset::new, BlockActionQuestAsset.BASE_CODEC)
            .append(new KeyedCodec<>("BlockToBreak", QuestItemFilter.CODEC), (asset, block) -> asset.blockToBreak = block, asset -> asset.blockToBreak)
            .add()
            .build();

    protected QuestItemFilter blockToBreak;

    private BreakBlockQuestAsset() {}

    @Override
    public BreakBlockQuestProgression.Builder builder() {
        return new BreakBlockQuestProgression.Builder(this);
    }

    /**
     * @return the blocks counted, unless a running quest names its own.
     */
    public QuestItemFilter getBlockToBreak() {
        return blockToBreak;
    }
}
