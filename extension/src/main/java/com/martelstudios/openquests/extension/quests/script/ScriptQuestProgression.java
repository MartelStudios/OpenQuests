package com.martelstudios.openquests.extension.quests.script;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;

/**
 * Runtime side of a script quest. No progression implemented, the life cycle is handled by custom scripts.
 */
public class ScriptQuestProgression extends AbstractQuestProgression<ScriptQuestProgression> {

    public static final BuilderCodec<ScriptQuestProgression> CODEC = BuilderCodec.builder(ScriptQuestProgression.class, ScriptQuestProgression::new, AbstractQuestProgression.BASE_CODEC)
                                                                                 .build();

    @Override
    public ScriptQuestAsset getAsset() {
        return (ScriptQuestAsset) super.getAsset();
    }
}
