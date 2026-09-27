package com.martelstudios.openquests.extension.constraints.condition;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.constraints.QuestConstraint;
import com.martelstudios.openquests.extension.journal.QuestPageService;

import javax.annotation.Nonnull;

/**
 * Quests that only move while the player's entity meets conditions of the game's own.
 */
public final class ConditionConstraintsFeature {
    public static final String ENTITY_CONDITION_TYPE_ID = "EntityCondition";

    private ConditionConstraintsFeature() {}

    /**
     * Registers the constraint under its type id, and how the journal describes it.
     */
    public static void register(@Nonnull JavaPlugin plugin) {
        QuestConstraint.CODEC.register(ENTITY_CONDITION_TYPE_ID, EntityConditionConstraint.class, EntityConditionConstraint.CODEC);

        QuestPageService.register(new EntityConditionRenderer());
    }
}
