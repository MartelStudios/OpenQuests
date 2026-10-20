package com.martelstudios.openquests.extension.quests.craft;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.OpenQuestsServices;

import javax.annotation.Nonnull;

/**
 * Craft a quantity of an item.
 */
public final class CraftFeature {
    public static final String TYPE_ID = "Craft";

    private CraftFeature() {}

    public static void register(@Nonnull JavaPlugin plugin, @Nonnull OpenQuestsServices services) {
        services.getQuestProgressionService().registerQuestType(TYPE_ID, CraftQuestAsset.class, CraftQuestAsset.CODEC, CraftQuestProgression.class, CraftQuestProgression.CODEC);

        plugin.getEntityStoreRegistry().registerSystem(new CraftRecipeEventSystem(services.getQuestProgressionService()));
    }
}
