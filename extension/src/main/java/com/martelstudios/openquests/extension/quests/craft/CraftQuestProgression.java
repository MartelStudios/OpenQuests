package com.martelstudios.openquests.extension.quests.craft;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.builtin.adventure.objectives.config.task.BlockTagOrItemIdField;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestProgression;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CraftQuestProgression extends QuantityQuestProgression<CraftQuestProgression> {

    public static final BuilderCodec<CraftQuestProgression> CODEC = BuilderCodec.builder(CraftQuestProgression.class, CraftQuestProgression::new, QuantityQuestProgression.BASE_CODEC)
                                                                                .append(new KeyedCodec<>("ItemToCraft", BlockTagOrItemIdField.CODEC), (quest, item) -> quest.itemToCraft = item, quest -> quest.itemToCraft)
                                                                                .add()
                                                                                .build();

    /**
     * Overrides the asset's item for this instance alone.
     */
    @Nullable
    protected BlockTagOrItemIdField itemToCraft;

    @Override
    public CraftQuestAsset getAsset() {
        return (CraftQuestAsset) super.getAsset();
    }

    /**
     * @return this instance's item if one was set on it, the asset's otherwise.
     */
    @Nonnull
    public BlockTagOrItemIdField getItemToCraft() {
        return itemToCraft != null ? itemToCraft : getAsset().getItemToCraft();
    }

    public CraftQuestProgression setItemToCraft(@Nullable BlockTagOrItemIdField itemToCraft) {
        this.itemToCraft = itemToCraft;
        return this;
    }

    @Nonnull
    @Override
    public Message getDefaultTitle() {
        var asset = getAsset();
        if (asset == null || asset.getItemToCraft() == null) return super.getDefaultTitle();

        return countedTitle("openquests.quest.default.craft", asset.getItemToCraft().getItemId());
    }
}
