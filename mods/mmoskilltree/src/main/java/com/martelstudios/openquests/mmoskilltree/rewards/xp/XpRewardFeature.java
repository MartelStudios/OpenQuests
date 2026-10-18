package com.martelstudios.openquests.mmoskilltree.rewards.xp;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.rewards.QuestReward;
import com.martelstudios.openquests.extension.journal.QuestPageService;

import javax.annotation.Nonnull;

/**
 * An amount of XP in an MMO Skill Tree skill.
 */
public final class XpRewardFeature {
    public static final String TYPE_ID = "MMOSkillTreeXp";

    private XpRewardFeature() {}

    /**
     * Registers the reward and its line in the journal.
     */
    public static void register(@Nonnull JavaPlugin plugin) {
        QuestReward.CODEC.register(TYPE_ID, MMOSkillTreeXpReward.class, MMOSkillTreeXpReward.CODEC);

        QuestPageService.register(new XpRewardRenderer());
    }
}
