package com.martelstudios.openquests.extension.quests.consumeitem;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.services.QuestProgressionService;

import javax.annotation.Nonnull;

/**
 * Eat or drink a quantity of an item.
 */
public final class ConsumeItemFeature {
    public static final String TYPE_ID = "Consume";

    private ConsumeItemFeature() {}

    public static void register(@Nonnull JavaPlugin plugin) {
        QuestProgressionService.get()
                               .registerQuestType(TYPE_ID, ConsumeItemQuestAsset.class, ConsumeItemQuestAsset.CODEC, ConsumeItemQuestProgression.class, ConsumeItemQuestProgression.CODEC);

        plugin.getEntityStoreRegistry().registerSystem(new ConsumeItemEventSystem());
    }
}
