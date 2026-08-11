package com.martelstudios.hyquests.models;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.hyquests.assets.InteractivelyPickupQuestAsset;
import com.martelstudios.hyquests.visitors.QuestVisitor;

public class InteractivelyPickupQuest extends CountQuest<InteractivelyPickupQuest> {

    public static final BuilderCodec<InteractivelyPickupQuest> CODEC = BuilderCodec.builder(InteractivelyPickupQuest.class, InteractivelyPickupQuest::new, CountQuest.BASE_CODEC)
                                                                                    .build();

    @Override
    public InteractivelyPickupQuestAsset getAsset() {
        return (InteractivelyPickupQuestAsset) super.getAsset();
    }
}
