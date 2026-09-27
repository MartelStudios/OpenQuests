package com.martelstudios.openquests.extension.quests.noop;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;

/**
 * Runtime side of a quest with no goal. Nothing here moves it: whatever ends it sets its state.
 */
public class NoOpQuestProgression extends AbstractQuestProgression<NoOpQuestProgression> {

    public static final BuilderCodec<NoOpQuestProgression> CODEC = BuilderCodec.builder(NoOpQuestProgression.class, NoOpQuestProgression::new, AbstractQuestProgression.BASE_CODEC)
                                                                               .build();

    @Override
    public NoOpQuestAsset getAsset() {
        return (NoOpQuestAsset) super.getAsset();
    }
}
