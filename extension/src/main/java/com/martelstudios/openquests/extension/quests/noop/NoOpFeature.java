package com.martelstudios.openquests.extension.quests.noop;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import com.martelstudios.openquests.core.services.QuestProgressionService;

import javax.annotation.Nonnull;

/**
 * A quest nothing in the game moves forward: a command, a reward, a constraint or a plugin ends it.
 */
public final class NoOpFeature {
    public static final String TYPE_ID = "NoOp";

    /**
     * The name this type shipped under, still read so that older assets and saved quests load.
     */
    public static final String LEGACY_TYPE_ID = "Script";

    private NoOpFeature() {}

    /**
     * The legacy name goes in first: the id a saved quest is written under is the last one its
     * class was registered with.
     */
    public static void register(@Nonnull JavaPlugin plugin) {
        OpenQuestAsset.CODEC.register(LEGACY_TYPE_ID, ScriptQuestAsset.class, ScriptQuestAsset.CODEC);
        AbstractQuestProgression.CODEC.register(LEGACY_TYPE_ID, NoOpQuestProgression.class, NoOpQuestProgression.CODEC);

        QuestProgressionService.get()
                               .registerQuestType(TYPE_ID, NoOpQuestAsset.class, NoOpQuestAsset.CODEC, NoOpQuestProgression.class, NoOpQuestProgression.CODEC);
    }
}
