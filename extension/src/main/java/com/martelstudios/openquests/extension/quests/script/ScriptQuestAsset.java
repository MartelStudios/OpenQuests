package com.martelstudios.openquests.extension.quests.script;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.QuestAsset;

/**
 * Succeeds when completed by other scripts.
 */
public class ScriptQuestAsset extends QuestAsset {

    public static final BuilderCodec<ScriptQuestAsset> CODEC = BuilderCodec.builder(ScriptQuestAsset.class, ScriptQuestAsset::new, QuestAsset.BASE_CODEC)
                                                                           .build();

    private ScriptQuestAsset() {}

    @Override
    public AbstractQuestProgression<?> create() {
        return new ScriptQuestProgression().setAssetId(getId());
    }
}
