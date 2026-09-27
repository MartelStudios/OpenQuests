package com.martelstudios.openquests.extension.quests.noop;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.core.models.OpenQuestAsset;

/**
 * A {@link NoOpQuestAsset} under the name it first shipped with. A class of its own, since the
 * asset editor describes each type through its codec and one codec cannot carry two type names.
 *
 * @deprecated write {@code "Type": "NoOp"} instead.
 */
@Deprecated
public class ScriptQuestAsset extends NoOpQuestAsset {

    public static final BuilderCodec<ScriptQuestAsset> CODEC = BuilderCodec.builder(ScriptQuestAsset.class, ScriptQuestAsset::new, OpenQuestAsset.BASE_CODEC)
                                                                           .build();

    private ScriptQuestAsset() {}
}
