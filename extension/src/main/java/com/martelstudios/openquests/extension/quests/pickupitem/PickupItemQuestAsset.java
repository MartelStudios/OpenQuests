package com.martelstudios.openquests.extension.quests.pickupitem;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.extension.quests.item.ItemExchangeQuestAsset;
import com.martelstudios.openquests.extension.quests.item.QuestItemFilter;

/**
 * Pick up a quantity of an item, off the ground or by hand.
 */
public class PickupItemQuestAsset extends ItemExchangeQuestAsset {

    public static final BuilderCodec<PickupItemQuestAsset> CODEC =
        BuilderCodec.builder(PickupItemQuestAsset.class, PickupItemQuestAsset::new, ItemExchangeQuestAsset.BASE_CODEC)
            .append(new KeyedCodec<>("ItemToPickup", QuestItemFilter.CODEC), (asset, item) -> asset.itemToPickup = item, asset -> asset.itemToPickup)
            .add()
            .build();

    protected QuestItemFilter itemToPickup;

    private PickupItemQuestAsset() {}

    @Override
    public AbstractQuestProgression<?> create() {
        return new PickupItemQuestProgression().setAssetId(getId());
    }

    /**
     * @return the items counted, unless a running quest names its own.
     */
    public QuestItemFilter getItemToPickup() {
        return itemToPickup;
    }
}
