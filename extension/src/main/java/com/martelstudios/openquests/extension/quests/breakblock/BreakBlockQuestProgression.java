package com.martelstudios.openquests.extension.quests.breakblock;

import com.hypixel.hytale.builtin.adventure.objectives.config.task.BlockTagOrItemIdField;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestProgression;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BreakBlockQuestProgression extends QuantityQuestProgression<BreakBlockQuestProgression> {

    public static final BuilderCodec<BreakBlockQuestProgression> CODEC = BuilderCodec.builder(BreakBlockQuestProgression.class, BreakBlockQuestProgression::new, QuantityQuestProgression.BASE_CODEC)
                                                                                   .append(new KeyedCodec<>("BlockToBreak", BlockTagOrItemIdField.CODEC), (quest, block) -> quest.blockToBreak = block, quest -> quest.blockToBreak)
                                                                                   .add()
                                                                                   .build();

    /**
     * Overrides the asset's block for this instance alone.
     */
    @Nullable
    protected BlockTagOrItemIdField blockToBreak;

    @Override
    public BreakBlockQuestAsset getAsset() {
        return (BreakBlockQuestAsset) super.getAsset();
    }

    /**
     * @return this instance's block if one was set on it, the asset's otherwise.
     */
    @Nonnull
    public BlockTagOrItemIdField getBlockToBreak() {
        return blockToBreak != null ? blockToBreak : getAsset().getBlockToBreak();
    }

    public BreakBlockQuestProgression setBlockToBreak(@Nullable BlockTagOrItemIdField blockToBreak) {
        this.blockToBreak = blockToBreak;
        return this;
    }
}
