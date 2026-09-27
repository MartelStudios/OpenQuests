package com.martelstudios.openquests.extension.quests.placeblock;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.extension.quests.block.PlacedBlockMarks;

import javax.annotation.Nonnull;

/**
 * Place a number of blocks.
 */
public final class PlaceBlockFeature {
    public static final String TYPE_ID = "PlaceBlock";

    private PlaceBlockFeature() {}

    public static void register(@Nonnull JavaPlugin plugin) {
        QuestProgressionService.get()
                               .registerQuestType(TYPE_ID, PlaceBlockQuestAsset.class, PlaceBlockQuestAsset.CODEC, PlaceBlockQuestProgression.class, PlaceBlockQuestProgression.CODEC);

        plugin.getEntityStoreRegistry().registerSystem(new PlaceBlockEventSystem());

        // Registered here, once, though break quests read the marks too: placing is what writes them
        PlacedBlockMarks.register(plugin.getChunkStoreRegistry());
    }
}
