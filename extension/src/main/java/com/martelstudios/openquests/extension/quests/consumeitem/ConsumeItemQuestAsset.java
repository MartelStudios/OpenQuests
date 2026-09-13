package com.martelstudios.openquests.extension.quests.consumeitem;

import com.hypixel.hytale.builtin.adventure.objectives.config.task.BlockTagOrItemIdField;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestAsset;

/**
 * Eat or drink a quantity of an item.
 */
public class ConsumeItemQuestAsset extends QuantityQuestAsset {

    public static final BuilderCodec<ConsumeItemQuestAsset> CODEC =
        BuilderCodec.builder(ConsumeItemQuestAsset.class, ConsumeItemQuestAsset::new, QuantityQuestAsset.BASE_CODEC)
                    .append(new KeyedCodec<>("ItemToConsume", BlockTagOrItemIdField.CODEC), (asset, item) -> asset.itemToConsume = item, asset -> asset.itemToConsume)
                    .add()
                    .build();

    protected BlockTagOrItemIdField itemToConsume;

    private ConsumeItemQuestAsset() {}

    @Override
    public AbstractQuestProgression<?> create() {
        return new ConsumeItemQuestProgression().setAssetId(getId());
    }

    public BlockTagOrItemIdField getItemToConsume() {
        return itemToConsume;
    }
}
