package com.martelstudios.openquests.extension.quests.wait;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.OpenQuestsServices;
import com.martelstudios.openquests.core.services.QuestProgressionService;

import javax.annotation.Nonnull;

public final class WaitFeature {
    public static final String TYPE_ID = "Wait";

    private WaitFeature() {}

    public static void register(@Nonnull JavaPlugin plugin, @Nonnull OpenQuestsServices services) {
        QuestProgressionService service = services.getQuestProgressionService();
        service.registerQuestType(TYPE_ID, WaitQuestAsset.class, WaitQuestAsset.CODEC, WaitQuestProgression.class, WaitQuestProgression.CODEC);
    }
}
