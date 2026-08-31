package com.martelstudios.openquests.extension.quests.script;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.services.QuestProgressionService;

import javax.annotation.Nonnull;

public final class ScriptFeature {
    public static final String TYPE_ID = "Script";

    private ScriptFeature() {}

    public static void register(@Nonnull JavaPlugin plugin) {
        QuestProgressionService.get()
                               .registerQuestType(TYPE_ID, ScriptQuestAsset.class, ScriptQuestAsset.CODEC, ScriptQuestProgression.class, ScriptQuestProgression.CODEC);
    }
}
