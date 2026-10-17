package com.martelstudios.openquests.mmoskilltree.quests.moment.completebounty;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.ziggfreed.common.quest.event.QuestClaimedEvent;

import javax.annotation.Nonnull;

/**
 * Collect a number of bounties, from one board or any, of one difficulty or any.
 */
public final class CompleteBountyFeature {
    public static final String TYPE_ID = "MMOSkillTreeCompleteBounty";

    private CompleteBountyFeature() {}

    /**
     * Registers the type and the claim listener feeding it.
     */
    public static void register(@Nonnull JavaPlugin plugin) {
        QuestProgressionService.get()
                               .registerQuestType(TYPE_ID, MMOSkillTreeCompleteBountyQuestAsset.class, MMOSkillTreeCompleteBountyQuestAsset.CODEC, CompleteBountyQuestProgression.class, CompleteBountyQuestProgression.CODEC);

        plugin.getEventRegistry().registerGlobal(QuestClaimedEvent.class, CompleteBountyEvents::handleQuestClaimed);
    }
}
