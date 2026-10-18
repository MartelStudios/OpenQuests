package com.martelstudios.openquests.mmoskilltree.rewards.level;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.rewards.QuestReward;
import com.martelstudios.openquests.extension.journal.QuestPageService;

import javax.annotation.Nonnull;

/**
 * Whole levels in an MMO Skill Tree skill.
 */
public final class LevelRewardFeature {
    public static final String TYPE_ID = "MMOSkillTreeLevel";

    private LevelRewardFeature() {}

    /**
     * Registers the reward and its line in the journal.
     */
    public static void register(@Nonnull JavaPlugin plugin) {
        QuestReward.CODEC.register(TYPE_ID, MMOSkillTreeLevelReward.class, MMOSkillTreeLevelReward.CODEC);

        QuestPageService.register(new LevelRewardRenderer());
    }
}
