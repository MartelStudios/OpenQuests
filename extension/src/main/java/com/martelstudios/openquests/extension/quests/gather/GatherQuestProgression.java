package com.martelstudios.openquests.extension.quests.gather;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.extension.quests.item.QuestItemFilter;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestProgression;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Quest whose progress is however many of a given item the player currently holds, rechecked
 * on every inventory change.
 */
public class GatherQuestProgression extends QuantityQuestProgression<GatherQuestProgression> {

    public static final BuilderCodec<GatherQuestProgression> CODEC = BuilderCodec.builder(GatherQuestProgression.class, GatherQuestProgression::new, QuantityQuestProgression.BASE_CODEC)
                                                                                 .append(new KeyedCodec<>("ItemToGather", QuestItemFilter.CODEC), (quest, item) -> quest.itemToGather = item, quest -> quest.itemToGather)
                                                                                 .add()
                                                                                 .build();

    @Nullable
    protected QuestItemFilter itemToGather;

    @Override
    public GatherQuestAsset getAsset() {
        return (GatherQuestAsset) super.getAsset();
    }

    /**
     * @return this instance's item if one was set on it, the asset's otherwise.
     */
    @Nonnull
    public QuestItemFilter getItemToGather() {
        return itemToGather != null ? itemToGather : getAsset().getItemToGather();
    }

    public GatherQuestProgression setItemToGather(@Nullable QuestItemFilter itemToGather) {
        this.itemToGather = itemToGather;
        return this;
    }

    @Nonnull
    @Override
    public Message getDefaultTitle() {
        var asset = getAsset();
        if (asset == null || asset.getItemToGather() == null) return super.getDefaultTitle();

        return countedTitle("openquests.quest.default.gather", asset.getItemToGather());
    }
}
