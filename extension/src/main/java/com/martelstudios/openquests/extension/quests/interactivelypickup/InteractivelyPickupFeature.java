package com.martelstudios.openquests.extension.quests.interactivelypickup;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.OpenQuestsServices;

import javax.annotation.Nonnull;

/**
 * Pick up a quantity of an item through the harvest interaction, unlike gather quests which
 * count whatever the player holds.
 */
public final class InteractivelyPickupFeature {
    public static final String TYPE_ID = "InteractivelyPickup";

    private InteractivelyPickupFeature() {}

    public static void register(@Nonnull JavaPlugin plugin, @Nonnull OpenQuestsServices services) {
        services.getQuestProgressionService().registerQuestType(TYPE_ID, InteractivelyPickupQuestAsset.class, InteractivelyPickupQuestAsset.CODEC, InteractivelyPickupQuestProgression.class, InteractivelyPickupQuestProgression.CODEC);

        plugin.getEntityStoreRegistry().registerSystem(new InteractivelyPickupItemEventSystem(services.getQuestProgressionService()));
    }
}
