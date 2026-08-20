package com.martelstudios.hyquests.extension.quests.useblock;

import com.hypixel.hytale.builtin.adventure.objectives.config.task.BlockTagOrItemIdField;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.hyquests.extension.quests.quantity.QuantityQuestProgression;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class UseBlockQuestProgression extends QuantityQuestProgression<UseBlockQuestProgression> {

    public static final BuilderCodec<UseBlockQuestProgression> CODEC = BuilderCodec.builder(UseBlockQuestProgression.class, UseBlockQuestProgression::new, QuantityQuestProgression.BASE_CODEC)
                                                                                   .append(new KeyedCodec<>("BlockToUse", BlockTagOrItemIdField.CODEC), (quest, block) -> quest.blockToUse = block, quest -> quest.blockToUse)
                                                                                   .add()
                                                                                   .build();

    /**
     * Overrides the asset's block for this instance alone.
     */
    @Nullable
    protected BlockTagOrItemIdField blockToUse;

    @Override
    public UseBlockQuestAsset getAsset() {
        return (UseBlockQuestAsset) super.getAsset();
    }

    /**
     * @return this instance's block if one was set on it, the asset's otherwise.
     */
    @Nonnull
    public BlockTagOrItemIdField getBlockToUse() {
        return blockToUse != null ? blockToUse : getAsset().getBlockToUse();
    }

    public UseBlockQuestProgression setBlockToUse(@Nullable BlockTagOrItemIdField blockToUse) {
        this.blockToUse = blockToUse;
        return this;
    }
}
