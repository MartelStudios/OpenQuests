package com.martelstudios.hyquests.extension.interactivelypickup;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.hyquests.core.visitors.QuestVisitor;
import com.martelstudios.hyquests.extension.count.CountQuest;

public class InteractivelyPickupQuest extends CountQuest<InteractivelyPickupQuest> {

    public static final BuilderCodec<InteractivelyPickupQuest> CODEC = BuilderCodec.builder(InteractivelyPickupQuest.class, InteractivelyPickupQuest::new, CountQuest.BASE_CODEC)
                                                                                    .build();

    @Override
    public InteractivelyPickupQuestAsset getAsset() {
        return (InteractivelyPickupQuestAsset) super.getAsset();
    }
}
