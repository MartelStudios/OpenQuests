package com.martelstudios.openquests.extension.quests.craft;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.extension.quests.item.QuestItemFilter;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestAsset;

/**
 * Craft a quantity of an item, whatever the recipe that produces it.
 */
public class CraftQuestAsset extends QuantityQuestAsset {

    public static final BuilderCodec<CraftQuestAsset> CODEC =
        BuilderCodec.builder(CraftQuestAsset.class, CraftQuestAsset::new, QuantityQuestAsset.BASE_CODEC)
                    .append(new KeyedCodec<>("ItemToCraft", QuestItemFilter.CODEC), (asset, item) -> asset.itemToCraft = item, asset -> asset.itemToCraft)
                    .add()
                    .build();

    protected QuestItemFilter itemToCraft;

    private CraftQuestAsset() {}

    @Override
    public AbstractQuestProgression<?> create() {
        return new CraftQuestProgression().setAssetId(getId());
    }

    public QuestItemFilter getItemToCraft() {
        return itemToCraft;
    }
}
