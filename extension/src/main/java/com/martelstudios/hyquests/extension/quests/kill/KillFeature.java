package com.martelstudios.hyquests.extension.quests.kill;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.hyquests.core.services.QuestProgressionService;

import javax.annotation.Nonnull;

/**
 * Kill quests, driven by the victim's death rather than by the damage that caused it.
 */
public final class KillFeature {
    public static final String KILL_PLAYER_TYPE_ID = "KillPlayer";

    private KillFeature() {}

    public static void register(@Nonnull JavaPlugin plugin) {
        QuestProgressionService.get()
                               .registerQuestType(KILL_PLAYER_TYPE_ID, KillPlayerQuestAsset.class, KillPlayerQuestAsset.CODEC, KillPlayerQuestProgression.class, KillPlayerQuestProgression.CODEC);

        plugin.getEntityStoreRegistry().registerSystem(new KillPlayerDeathSystem());
    }
}
