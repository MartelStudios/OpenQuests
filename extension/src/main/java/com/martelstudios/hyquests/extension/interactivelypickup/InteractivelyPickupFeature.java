package com.martelstudios.hyquests.extension.interactivelypickup;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.hyquests.core.services.QuestProgressionService;

import javax.annotation.Nonnull;

/**
 * Pick up a quantity of an item through the harvest interaction, unlike gather quests which
 * count whatever the player holds.
 */
public final class InteractivelyPickupFeature {
    public static final String TYPE_ID = "InteractivelyPickup";

    private InteractivelyPickupFeature() {}

    public static void register(@Nonnull JavaPlugin plugin) {
        QuestProgressionService.get()
                               .registerQuestType(TYPE_ID, InteractivelyPickupQuestAsset.class, InteractivelyPickupQuestAsset.CODEC, InteractivelyPickupQuest.class, InteractivelyPickupQuest.CODEC);

        plugin.getEntityStoreRegistry().registerSystem(new InteractivelyPickupItemEventSystem());
    }
}
