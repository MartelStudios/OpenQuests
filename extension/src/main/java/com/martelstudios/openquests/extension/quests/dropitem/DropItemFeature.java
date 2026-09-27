package com.martelstudios.openquests.extension.quests.dropitem;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.services.QuestProgressionService;

import javax.annotation.Nonnull;

/**
 * Throw a quantity of an item out of the inventory, counted at the moment it leaves the hand
 * rather than read from what the player holds.
 */
public final class DropItemFeature {
    public static final String TYPE_ID = "DropItem";

    private DropItemFeature() {}

    /**
     * Registers the type and the two systems a throw goes through, which share what was asked.
     */
    public static void register(@Nonnull JavaPlugin plugin) {
        QuestProgressionService.get()
                               .registerQuestType(TYPE_ID, DropItemQuestAsset.class, DropItemQuestAsset.CODEC, DropItemQuestProgression.class, DropItemQuestProgression.CODEC);

        DropItemSystems systems = new DropItemSystems();
        plugin.getEntityStoreRegistry().registerSystem(systems.requestSystem());
        plugin.getEntityStoreRegistry().registerSystem(systems.dropSystem());
    }
}
