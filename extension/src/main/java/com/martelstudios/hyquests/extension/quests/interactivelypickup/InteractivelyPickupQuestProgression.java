package com.martelstudios.hyquests.extension.quests.interactivelypickup;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.hyquests.extension.quests.count.CountQuestProgression;

public class InteractivelyPickupQuestProgression extends CountQuestProgression<InteractivelyPickupQuestProgression> {

    public static final BuilderCodec<InteractivelyPickupQuestProgression> CODEC = BuilderCodec.builder(InteractivelyPickupQuestProgression.class, InteractivelyPickupQuestProgression::new, CountQuestProgression.BASE_CODEC)
                                                                                              .build();

    @Override
    public InteractivelyPickupQuestAsset getAsset() {
        return (InteractivelyPickupQuestAsset) super.getAsset();
    }
}
