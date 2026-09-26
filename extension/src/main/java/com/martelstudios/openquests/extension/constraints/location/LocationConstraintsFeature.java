package com.martelstudios.openquests.extension.constraints.location;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.constraints.QuestConstraint;

import javax.annotation.Nonnull;

/**
 * Quests that only move in one place: a world named alike, or an area around a position.
 */
public final class LocationConstraintsFeature {
    public static final String IN_WORLD_TYPE_ID = "InWorld";
    public static final String NEAR_POSITION_TYPE_ID = "NearPosition";

    private LocationConstraintsFeature() {}

    /**
     * Registers both constraints under their type ids.
     */
    public static void register(@Nonnull JavaPlugin plugin) {
        QuestConstraint.CODEC.register(IN_WORLD_TYPE_ID, InWorldConstraint.class, InWorldConstraint.CODEC);
        QuestConstraint.CODEC.register(NEAR_POSITION_TYPE_ID, NearPositionConstraint.class, NearPositionConstraint.CODEC);
    }
}
