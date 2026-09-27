package com.martelstudios.openquests.extension.quests.dropitem;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.Message;
import com.martelstudios.openquests.extension.quests.item.ItemExchange;
import com.martelstudios.openquests.extension.quests.item.ItemExchangeQuestProgression;
import com.martelstudios.openquests.extension.quests.item.QuestItemFilter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class DropItemQuestProgression extends ItemExchangeQuestProgression<DropItemQuestProgression> {

    public static final BuilderCodec<DropItemQuestProgression> CODEC = BuilderCodec.builder(DropItemQuestProgression.class, DropItemQuestProgression::new, ItemExchangeQuestProgression.BASE_CODEC)
                                                                              .append(new KeyedCodec<>("ItemToDrop", QuestItemFilter.CODEC), (quest, item) -> quest.itemToDrop = item, quest -> quest.itemToDrop)
                                                                              .add()
                                                                              .build();

    @Nullable
    protected QuestItemFilter itemToDrop;

    @Override
    public DropItemQuestAsset getAsset() {
        return (DropItemQuestAsset) super.getAsset();
    }

    /**
     * @return this instance's item if one was set on it, the asset's otherwise.
     */
    @Nonnull
    public QuestItemFilter getItemToDrop() {
        return itemToDrop != null ? itemToDrop : getAsset().getItemToDrop();
    }

    /**
     * Overrides the asset's item for this instance only.
     */
    public DropItemQuestProgression setItemToDrop(@Nullable QuestItemFilter itemToDrop) {
        this.itemToDrop = itemToDrop;
        return this;
    }

    @Nonnull
    @Override
    public QuestItemFilter getItemFilter() {
        return getItemToDrop();
    }

    @Override
    protected boolean counts(@Nonnull ItemExchange exchange) {
        return exchange == ItemExchange.THROW;
    }

    @Nonnull
    @Override
    public Message getDefaultTitle() {
        var asset = getAsset();
        if (asset == null || asset.getItemToDrop() == null) return super.getDefaultTitle();

        return countedTitle("openquests.quest.default.drop", asset.getItemToDrop());
    }
}
