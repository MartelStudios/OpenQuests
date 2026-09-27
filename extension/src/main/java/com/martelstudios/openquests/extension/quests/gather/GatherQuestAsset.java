package com.martelstudios.openquests.extension.quests.gather;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.extension.quests.item.QuestItemFilter;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestAsset;

public class GatherQuestAsset extends QuantityQuestAsset {

    public static final BuilderCodec<GatherQuestAsset> CODEC =
        BuilderCodec.builder(GatherQuestAsset.class, GatherQuestAsset::new, QuantityQuestAsset.BASE_CODEC)
            .append(new KeyedCodec<>("ItemToGather", QuestItemFilter.CODEC), (asset, item) -> asset.itemToGather = item, asset -> asset.itemToGather)
            .add()
            .build();

    protected QuestItemFilter itemToGather;

    private GatherQuestAsset() {}

    @Override
    public AbstractQuestProgression<?> create() {
        return new GatherQuestProgression().setAssetId(getId());
    }

    public QuestItemFilter getItemToGather() {
        return itemToGather;
    }
}
