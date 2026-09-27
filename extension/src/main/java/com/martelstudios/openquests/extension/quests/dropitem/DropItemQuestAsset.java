package com.martelstudios.openquests.extension.quests.dropitem;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.extension.quests.item.ItemExchangeQuestAsset;
import com.martelstudios.openquests.extension.quests.item.QuestItemFilter;

/**
 * Throw a quantity of an item out of the inventory.
 */
public class DropItemQuestAsset extends ItemExchangeQuestAsset {

    public static final BuilderCodec<DropItemQuestAsset> CODEC =
        BuilderCodec.builder(DropItemQuestAsset.class, DropItemQuestAsset::new, ItemExchangeQuestAsset.BASE_CODEC)
            .append(new KeyedCodec<>("ItemToDrop", QuestItemFilter.CODEC), (asset, item) -> asset.itemToDrop = item, asset -> asset.itemToDrop)
            .add()
            .build();

    protected QuestItemFilter itemToDrop;

    private DropItemQuestAsset() {}

    @Override
    public AbstractQuestProgression<?> create() {
        return new DropItemQuestProgression().setAssetId(getId());
    }

    /**
     * @return the items counted, unless a running quest names its own.
     */
    public QuestItemFilter getItemToDrop() {
        return itemToDrop;
    }
}
