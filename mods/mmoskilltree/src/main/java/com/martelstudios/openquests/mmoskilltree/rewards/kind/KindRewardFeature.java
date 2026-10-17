package com.martelstudios.openquests.mmoskilltree.rewards.kind;

import com.hypixel.hytale.server.core.asset.LoadAssetEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.rewards.QuestReward;
import com.martelstudios.openquests.extension.journal.QuestPageService;
import com.ziggfreed.common.loot.reward.RewardKinds;

import javax.annotation.Nonnull;

/**
 * Any reward kind of MMO Skill Tree's CommonLib, such as mastery points or the Mastery Trainer's
 * favour.
 */
public final class KindRewardFeature {
    public static final String TYPE_ID = "MMOSkillTreeReward";

    private KindRewardFeature() {}

    /**
     * Registers the reward, its line in the journal and the check of its kind.
     */
    public static void register(@Nonnull JavaPlugin plugin) {
        QuestReward.CODEC.register(TYPE_ID, KindReward.class, KindReward.CODEC);

        QuestPageService.register(new KindRewardRenderer());
        plugin.getEventRegistry().registerGlobal(LoadAssetEvent.PRIORITY_LOAD_LATE, LoadAssetEvent.class, KindRewardFeature::handleLoadAsset);
    }

    /**
     * Fails the load once per reward naming a kind nothing registered. Kinds register as plugins
     * set up, so all of them are known by now.
     */
    private static void handleLoadAsset(@Nonnull LoadAssetEvent event) {
        for (OpenQuestAsset asset : OpenQuestAsset.getAssetMap().getAssetMap().values()) {
            for (QuestState state : QuestState.values()) {
                for (QuestReward reward : asset.getRewards(state)) {
                    if (reward instanceof KindReward kindReward && !RewardKinds.shared().isRegistered(kindReward.getKind())) {
                        event.failed(true, "Quest asset '" + asset.getId() + "' names an unknown CommonLib reward kind '" + kindReward.getKind() + "'");
                    }
                }
            }
        }
    }
}
