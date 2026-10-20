package com.martelstudios.openquests.extension.constraints.condition;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.OpenQuestsServices;
import com.martelstudios.openquests.core.constraints.QuestConstraint;
import com.martelstudios.openquests.extension.journal.QuestPageService;

import javax.annotation.Nonnull;

/**
 * Quests that only move while the player's entity meets conditions of the game's own, or while
 * their quests from another asset stand as asked.
 */
public final class ConditionConstraintsFeature {
    public static final String ENTITY_CONDITION_TYPE_ID = "EntityCondition";
    public static final String REQUIRES_QUEST_TYPE_ID = "RequiresQuest";

    private ConditionConstraintsFeature() {}

    /**
     * Registers both constraints under their type ids, and how the journal describes them.
     */
    public static void register(@Nonnull JavaPlugin plugin, @Nonnull OpenQuestsServices services) {
        QuestConstraint.CODEC.register(ENTITY_CONDITION_TYPE_ID, EntityConditionConstraint.class, EntityConditionConstraint.CODEC);

        QuestConstraint.CODEC.register(REQUIRES_QUEST_TYPE_ID, RequiresQuestConstraint.class, RequiresQuestConstraint.CODEC);

        QuestPageService.register(new EntityConditionRenderer());
        QuestPageService.register(new RequiresQuestRenderer());
    }
}
