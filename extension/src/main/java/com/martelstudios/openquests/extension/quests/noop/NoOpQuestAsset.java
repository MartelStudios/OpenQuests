package com.martelstudios.openquests.extension.quests.noop;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.OpenQuestAsset;

/**
 * Has no goal of its own. Ends through a command, a reward, a constraint or a plugin.
 */
public class NoOpQuestAsset extends OpenQuestAsset {

    public static final BuilderCodec<NoOpQuestAsset> CODEC = BuilderCodec.builder(NoOpQuestAsset.class, NoOpQuestAsset::new, OpenQuestAsset.BASE_CODEC)
                                                                         .build();

    protected NoOpQuestAsset() {}

    @Override
    public AbstractQuestProgression<?> create() {
        return new NoOpQuestProgression().setAssetId(getId());
    }
}
