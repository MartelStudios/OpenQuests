package com.martelstudios.openquests.extension.quests.noop;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.services.QuestProgressionService;

import javax.annotation.Nonnull;

/**
 * A quest nothing in the game moves forward: a command, a reward, a constraint or a plugin ends it.
 */
public final class NoOpFeature {
    public static final String TYPE_ID = "NoOp";

    private NoOpFeature() {}

    /**
     * Registers the type alone: no system watches the game on its behalf.
     */
    public static void register(@Nonnull JavaPlugin plugin) {
        QuestProgressionService.get()
                               .registerQuestType(TYPE_ID, NoOpQuestAsset.class, NoOpQuestAsset.CODEC, NoOpQuestProgression.class, NoOpQuestProgression.CODEC);
    }
}
