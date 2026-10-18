package com.martelstudios.openquests.mmoskilltree.skills;

import com.hypixel.hytale.server.core.asset.LoadAssetEvent;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.rewards.QuestReward;
import com.ziggfreed.mmoskilltree.skill.SkillRegistry;

import javax.annotation.Nonnull;

/**
 * Fails the load once per skill MMO Skill Tree does not know, on a quest asset or any of its
 * rewards, so a pack with several mistakes names them all.
 */
public final class SkillReferenceValidator {

    private SkillReferenceValidator() {}

    /**
     * Runs late, once custom skills are known.
     */
    public static void handleLoadAsset(@Nonnull LoadAssetEvent event) {
        for (OpenQuestAsset asset : OpenQuestAsset.getAssetMap().getAssetMap().values()) {
            check(event, asset.getId(), asset);

            for (QuestState state : QuestState.values()) {
                for (QuestReward reward : asset.getRewards(state)) check(event, asset.getId(), reward);
            }
        }
    }

    private static void check(@Nonnull LoadAssetEvent event, @Nonnull String assetId, @Nonnull Object holder) {
        if (!(holder instanceof SkillReference reference)) return;

        String skill = reference.getSkill();
        if (skill != null && !SkillRegistry.getInstance().isKnownSkill(skill)) {
            event.failed(true, "Quest asset '" + assetId + "' names an unknown MMO Skill Tree skill '" + skill + "'");
        }
    }
}
