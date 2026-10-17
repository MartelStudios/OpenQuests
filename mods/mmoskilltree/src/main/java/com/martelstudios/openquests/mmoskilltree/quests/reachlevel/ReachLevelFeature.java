package com.martelstudios.openquests.mmoskilltree.quests.reachlevel;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.scopes.player.events.QuestAddedToPlayerStoreEvent;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.ziggfreed.mmoskilltree.api.MMOSkillTreeAPI;

import javax.annotation.Nonnull;

/**
 * Reach a level in an MMO Skill Tree skill.
 */
public final class ReachLevelFeature {
    public static final String TYPE_ID = "MMOSkillTreeReachLevel";

    private ReachLevelFeature() {}

    /**
     * Registers the type and keeps its quests on the player's level.
     */
    public static void register(@Nonnull JavaPlugin plugin) {
        QuestProgressionService.get()
                               .registerQuestType(TYPE_ID, MMOSkillTreeReachLevelQuestAsset.class, MMOSkillTreeReachLevelQuestAsset.CODEC, ReachLevelQuestProgression.class, ReachLevelQuestProgression.CODEC);

        MMOSkillTreeAPI.addXpListener(ReachLevelEvents.XP_LISTENER);
        plugin.getEventRegistry().registerGlobal(QuestAddedToPlayerStoreEvent.class, ReachLevelEvents::handleQuestAssignedToPlayer);
    }

    /**
     * MMO Skill Tree's listeners outlive a plugin that shuts down, so this one is taken back by hand.
     */
    public static void unregister() {
        MMOSkillTreeAPI.removeXpListener(ReachLevelEvents.XP_LISTENER);
    }
}
