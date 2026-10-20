package com.martelstudios.openquests.mmoskilltree.quests.reachlevel;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.OpenQuestsServices;
import com.martelstudios.openquests.core.scopes.player.events.QuestAddedToPlayerStoreEvent;
import com.ziggfreed.mmoskilltree.api.MMOSkillTreeAPI;

import javax.annotation.Nonnull;

/**
 * Reach a level in an MMO Skill Tree skill.
 */
public final class ReachLevelFeature {

    /**
     * The listener added, taken back on shutdown: MMO Skill Tree keeps its listeners by identity.
     */
    private static ReachLevelEvents events;
    public static final String TYPE_ID = "MMOSkillTreeReachLevel";

    private ReachLevelFeature() {}

    /**
     * Registers the type and keeps its quests on the player's level.
     */
    public static void register(@Nonnull JavaPlugin plugin, @Nonnull OpenQuestsServices services) {
        services.getQuestProgressionService().registerQuestType(TYPE_ID, MMOSkillTreeReachLevelQuestAsset.class, MMOSkillTreeReachLevelQuestAsset.CODEC, ReachLevelQuestProgression.class, ReachLevelQuestProgression.CODEC);

        events = new ReachLevelEvents(services.getQuestProgressionService(), services.getPlatform().players());
        MMOSkillTreeAPI.addXpListener(events.xpListener());
        plugin.getEventRegistry().registerGlobal(QuestAddedToPlayerStoreEvent.class, events::handleQuestAssignedToPlayer);
    }

    /**
     * MMO Skill Tree's listeners outlive a plugin that shuts down, so this one is taken back by hand.
     */
    public static void unregister() {
        if (events != null) MMOSkillTreeAPI.removeXpListener(events.xpListener());
    }
}
