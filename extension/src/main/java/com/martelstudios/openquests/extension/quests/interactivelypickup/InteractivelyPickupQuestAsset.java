package com.martelstudios.openquests.extension.quests.interactivelypickup;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.extension.quests.item.QuestItemFilter;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestAsset;

public class InteractivelyPickupQuestAsset extends QuantityQuestAsset {

    public static final BuilderCodec<InteractivelyPickupQuestAsset> CODEC =
        BuilderCodec.builder(InteractivelyPickupQuestAsset.class, InteractivelyPickupQuestAsset::new, QuantityQuestAsset.BASE_CODEC)
            .append(new KeyedCodec<>("ItemToPickup", QuestItemFilter.CODEC), (asset, item) -> asset.itemToPickup = item, asset -> asset.itemToPickup)
            .add()
            .build();

    protected QuestItemFilter itemToPickup;

    private InteractivelyPickupQuestAsset() {}

    @Override
    public InteractivelyPickupQuestProgression.Builder builder() {
        return new InteractivelyPickupQuestProgression.Builder(this);
    }

    public QuestItemFilter getItemToPickup() {
        return itemToPickup;
    }
}
