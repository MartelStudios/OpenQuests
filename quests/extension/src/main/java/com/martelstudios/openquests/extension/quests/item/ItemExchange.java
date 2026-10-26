package com.martelstudios.openquests.extension.quests.item;

/**
 * How an item crossed between the player's inventory and the world.
 */
public enum ItemExchange {
    /**
     * Thrown out of the inventory on purpose.
     */
    THROW,

    /**
     * Picked up off the ground, where it may have been the player's own throw.
     */
    GROUND_PICKUP,

    /**
     * Harvested by hand from a block, which a throw can never hand back.
     */
    HARVEST
}
