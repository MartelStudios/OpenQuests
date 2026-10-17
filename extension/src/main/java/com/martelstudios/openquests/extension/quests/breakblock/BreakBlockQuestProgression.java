package com.martelstudios.openquests.extension.quests.breakblock;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.extension.quests.block.BlockAction;
import com.martelstudios.openquests.extension.quests.block.BlockActionQuestProgression;
import com.martelstudios.openquests.extension.quests.item.QuestItemFilter;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestProgression;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BreakBlockQuestProgression extends BlockActionQuestProgression<BreakBlockQuestProgression> {

    public static final BuilderCodec<BreakBlockQuestProgression> CODEC = BuilderCodec.builder(BreakBlockQuestProgression.class, BreakBlockQuestProgression::new, BlockActionQuestProgression.BASE_CODEC)
                                                                              .append(new KeyedCodec<>("BlockToBreak", QuestItemFilter.CODEC), (quest, block) -> quest.blockToBreak = block, quest -> quest.blockToBreak)
                                                                              .add()
                                                                              .build();

    /**
     * Overrides the asset's block for this instance alone.
     */
    @Nullable
    protected QuestItemFilter blockToBreak;

    @Override
    public BreakBlockQuestAsset getAsset() {
        return (BreakBlockQuestAsset) super.getAsset();
    }

    /**
     * @return this instance's block if one was set on it, the asset's otherwise.
     */
    @Nonnull
    public QuestItemFilter getBlockToBreak() {
        return blockToBreak != null ? blockToBreak : getAsset().getBlockToBreak();
    }

    protected BreakBlockQuestProgression() {}

    /**
     * Sets up a break quest, whose block can be overridden for that quest alone.
     */
    public static final class Builder extends QuantityQuestProgression.Builder<BreakBlockQuestProgression, Builder> {
        Builder(@Nonnull BreakBlockQuestAsset asset) {
            super(new BreakBlockQuestProgression(), asset.getId());
        }

        /**
         * Overrides the asset's block for this quest alone.
         */
        public Builder blockToBreak(@Nullable QuestItemFilter blockToBreak) {
            quest().blockToBreak = blockToBreak;
            return this;
        }
    }

    @Nonnull
    @Override
    public QuestItemFilter getBlockFilter() {
        return getBlockToBreak();
    }

    @Override
    protected boolean counts(@Nonnull BlockAction action) {
        return action == BlockAction.BREAK;
    }
}
