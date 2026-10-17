package com.martelstudios.openquests.mmoskilltree.rewards.xp;

import com.hypixel.hytale.server.core.asset.LoadAssetEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.rewards.QuestReward;
import com.martelstudios.openquests.extension.journal.QuestPageService;
import com.ziggfreed.mmoskilltree.skill.SkillRegistry;

import javax.annotation.Nonnull;

/**
 * XP in an MMO Skill Tree skill, as an amount or as whole levels.
 */
public final class XpRewardFeature {
    public static final String XP_TYPE_ID = "MMOSkillTreeXp";
    public static final String LEVEL_TYPE_ID = "MMOSkillTreeLevel";

    private XpRewardFeature() {}

    /**
     * Registers both rewards, their lines in the journal and the check of their skill.
     */
    public static void register(@Nonnull JavaPlugin plugin) {
        QuestReward.CODEC.register(XP_TYPE_ID, MMOSkillTreeXpReward.class, MMOSkillTreeXpReward.CODEC);
        QuestReward.CODEC.register(LEVEL_TYPE_ID, MMOSkillTreeLevelReward.class, MMOSkillTreeLevelReward.CODEC);

        QuestPageService.register(new XpRewardRenderer());
        QuestPageService.register(new LevelRewardRenderer());
        plugin.getEventRegistry().registerGlobal(LoadAssetEvent.PRIORITY_LOAD_LATE, LoadAssetEvent.class, XpRewardFeature::handleLoadAsset);
    }

    /**
     * Fails the load once per reward naming a skill MMO Skill Tree does not know.
     */
    private static void handleLoadAsset(@Nonnull LoadAssetEvent event) {
        for (OpenQuestAsset asset : OpenQuestAsset.getAssetMap().getAssetMap().values()) {
            for (QuestState state : QuestState.values()) {
                for (QuestReward reward : asset.getRewards(state)) {
                    String skill = reward instanceof MMOSkillTreeXpReward xp ? xp.getSkill() : reward instanceof MMOSkillTreeLevelReward level ? level.getSkill() : null;

                    if (skill != null && !SkillRegistry.getInstance().isKnownSkill(skill)) {
                        event.failed(true, "Quest asset '" + asset.getId() + "' rewards an unknown MMO Skill Tree skill '" + skill + "'");
                    }
                }
            }
        }
    }
}
