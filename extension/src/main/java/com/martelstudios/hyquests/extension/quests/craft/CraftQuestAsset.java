package com.martelstudios.hyquests.extension.quests.craft;

import com.hypixel.hytale.builtin.adventure.objectives.config.task.BlockTagOrItemIdField;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.hyquests.core.models.AbstractQuestProgression;
import com.martelstudios.hyquests.extension.quests.quantity.QuantityQuestAsset;

/**
 * Craft a quantity of an item, whatever the recipe that produces it.
 */
public class CraftQuestAsset extends QuantityQuestAsset {

    public static final BuilderCodec<CraftQuestAsset> CODEC =
        BuilderCodec.builder(CraftQuestAsset.class, CraftQuestAsset::new, QuantityQuestAsset.BASE_CODEC)
                    .append(new KeyedCodec<>("ItemToCraft", BlockTagOrItemIdField.CODEC), (asset, item) -> asset.itemToCraft = item, asset -> asset.itemToCraft)
                    .add()
                    .build();

    protected BlockTagOrItemIdField itemToCraft;

    private CraftQuestAsset() {}

    @Override
    public AbstractQuestProgression<?> create() {
        return new CraftQuestProgression().setAssetId(getId());
    }

    public BlockTagOrItemIdField getItemToCraft() {
        return itemToCraft;
    }
}
