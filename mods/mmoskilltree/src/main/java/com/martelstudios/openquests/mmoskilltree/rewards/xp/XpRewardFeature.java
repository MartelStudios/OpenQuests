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
 * XP in an MMO Skill Tree skill.
 */
public final class XpRewardFeature {
    public static final String TYPE_ID = "MMOSkillTreeXp";

    private XpRewardFeature() {}

    /**
     * Registers the reward, its line in the journal and the check of its skill.
     */
    public static void register(@Nonnull JavaPlugin plugin) {
        QuestReward.CODEC.register(TYPE_ID, XpReward.class, XpReward.CODEC);

        QuestPageService.register(new XpRewardRenderer());
        plugin.getEventRegistry().registerGlobal(LoadAssetEvent.PRIORITY_LOAD_LATE, LoadAssetEvent.class, XpRewardFeature::handleLoadAsset);
    }

    /**
     * Fails the load once per reward naming a skill MMO Skill Tree does not know.
     */
    private static void handleLoadAsset(@Nonnull LoadAssetEvent event) {
        for (OpenQuestAsset asset : OpenQuestAsset.getAssetMap().getAssetMap().values()) {
            for (QuestState state : QuestState.values()) {
                for (QuestReward reward : asset.getRewards(state)) {
                    if (reward instanceof XpReward xp && !SkillRegistry.getInstance().isKnownSkill(xp.getSkill())) {
                        event.failed(true, "Quest asset '" + asset.getId() + "' rewards XP in an unknown MMO Skill Tree skill '" + xp.getSkill() + "'");
                    }
                }
            }
        }
    }
}
