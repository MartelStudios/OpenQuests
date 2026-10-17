package com.martelstudios.openquests.extension.quests.consumeitem;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.extension.quests.item.QuestItemFilter;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestAsset;

/**
 * Eat or drink a quantity of an item.
 */
public class ConsumeItemQuestAsset extends QuantityQuestAsset {

    public static final BuilderCodec<ConsumeItemQuestAsset> CODEC =
        BuilderCodec.builder(ConsumeItemQuestAsset.class, ConsumeItemQuestAsset::new, QuantityQuestAsset.BASE_CODEC)
                    .append(new KeyedCodec<>("ItemToConsume", QuestItemFilter.CODEC), (asset, item) -> asset.itemToConsume = item, asset -> asset.itemToConsume)
                    .add()
                    .build();

    protected QuestItemFilter itemToConsume;

    private ConsumeItemQuestAsset() {}

    @Override
    public ConsumeItemQuestProgression.Builder builder() {
        return new ConsumeItemQuestProgression.Builder(this);
    }

    public QuestItemFilter getItemToConsume() {
        return itemToConsume;
    }
}
