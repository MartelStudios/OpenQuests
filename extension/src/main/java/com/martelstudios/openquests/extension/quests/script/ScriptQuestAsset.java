package com.martelstudios.openquests.extension.quests.script;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.OpenQuestAsset;

/**
 * Succeeds when completed by other scripts.
 */
public class ScriptQuestAsset extends OpenQuestAsset {

    public static final BuilderCodec<ScriptQuestAsset> CODEC = BuilderCodec.builder(ScriptQuestAsset.class, ScriptQuestAsset::new, OpenQuestAsset.BASE_CODEC)
                                                                           .build();

    private ScriptQuestAsset() {}

    @Override
    public AbstractQuestProgression<?> create() {
        return new ScriptQuestProgression().setAssetId(getId());
    }
}
