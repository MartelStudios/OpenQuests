package com.martelstudios.openquests.extension.quests.craft;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.extension.quests.item.QuestItemFilter;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestProgression;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CraftQuestProgression extends QuantityQuestProgression<CraftQuestProgression> {

    public static final BuilderCodec<CraftQuestProgression> CODEC = BuilderCodec.builder(CraftQuestProgression.class, CraftQuestProgression::new, QuantityQuestProgression.BASE_CODEC)
                                                                                .append(new KeyedCodec<>("ItemToCraft", QuestItemFilter.CODEC), (quest, item) -> quest.itemToCraft = item, quest -> quest.itemToCraft)
                                                                                .add()
                                                                                .build();

    /**
     * Overrides the asset's item for this instance alone.
     */
    @Nullable
    protected QuestItemFilter itemToCraft;

    @Override
    public CraftQuestAsset getAsset() {
        return (CraftQuestAsset) super.getAsset();
    }

    /**
     * @return this instance's item if one was set on it, the asset's otherwise.
     */
    @Nonnull
    public QuestItemFilter getItemToCraft() {
        return itemToCraft != null ? itemToCraft : getAsset().getItemToCraft();
    }

    protected CraftQuestProgression() {}

    /**
     * Sets up a craft quest, whose item can be overridden for that quest alone.
     */
    public static final class Builder extends QuantityQuestProgression.Builder<CraftQuestProgression, Builder> {
        Builder(@Nonnull CraftQuestAsset asset) {
            super(new CraftQuestProgression(), asset.getId());
        }

        /**
         * Overrides the asset's item for this quest alone.
         */
        public Builder itemToCraft(@Nullable QuestItemFilter itemToCraft) {
            quest().itemToCraft = itemToCraft;
            return this;
        }
    }

    @Nonnull
    @Override
    public Message getDefaultTitle() {
        var asset = getAsset();
        if (asset == null || asset.getItemToCraft() == null) return super.getDefaultTitle();

        return countedTitle("openquests.quest.default.craft", asset.getItemToCraft());
    }
}
