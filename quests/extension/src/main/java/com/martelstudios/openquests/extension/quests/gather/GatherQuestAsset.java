package com.martelstudios.openquests.extension.quests.gather;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.extension.quests.item.QuestItemFilter;
import com.martelstudios.openquests.extension.quests.quantity.HeldQuantityQuestAsset;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestAsset;

public class GatherQuestAsset extends HeldQuantityQuestAsset {

    public static final BuilderCodec<GatherQuestAsset> CODEC =
        BuilderCodec.builder(GatherQuestAsset.class, GatherQuestAsset::new, QuantityQuestAsset.BASE_CODEC)
            .append(new KeyedCodec<>("ItemToGather", QuestItemFilter.CODEC), (asset, item) -> asset.itemToGather = item, asset -> asset.itemToGather)
            .documentation("Filter for items counted in the player's inventory.")
            .add()
            .documentation("Defines a quest for holding a target quantity of items.")
            .build();

    protected QuestItemFilter itemToGather;

    private GatherQuestAsset() {}

    @Override
    public GatherQuestProgression.Builder builder() {
        return new GatherQuestProgression.Builder(this);
    }

    public QuestItemFilter getItemToGather() {
        return itemToGather;
    }
}
