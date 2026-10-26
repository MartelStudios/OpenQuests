package com.martelstudios.openquests.extension.quests.pickupitem;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.OpenQuestsServices;

import javax.annotation.Nonnull;

/**
 * Pick up a quantity of an item, from the ground or by hand, counted at the moment it reaches the
 * inventory rather than read from what the player holds.
 */
public final class PickupItemFeature {
    public static final String TYPE_ID = "PickupItem";

    private PickupItemFeature() {}

    /**
     * Registers the type and the systems that tell a pickup apart, which share what they saw.
     */
    public static void register(@Nonnull JavaPlugin plugin, @Nonnull OpenQuestsServices services) {
        services.getQuestProgressionService().registerQuestType(TYPE_ID, PickupItemQuestAsset.class, PickupItemQuestAsset.CODEC, PickupItemQuestProgression.class, PickupItemQuestProgression.CODEC);

        PickupItemSystems systems = new PickupItemSystems(services.getQuestProgressionService());
        plugin.getEntityStoreRegistry().registerSystem(systems.leftGroundSystem());
        plugin.getEntityStoreRegistry().registerSystem(systems.flyingItemSystem());
        plugin.getEntityStoreRegistry().registerSystem(systems.handPickupSystem());
    }
}
