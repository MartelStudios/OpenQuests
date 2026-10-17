package com.martelstudios.openquests.mmoskilltree.quests.moment.gainxp;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.ziggfreed.mmoskilltree.api.MMOSkillTreeAPI;

import javax.annotation.Nonnull;

/**
 * Gain an amount of XP, in one skill or in any.
 */
public final class GainXpFeature {
    public static final String TYPE_ID = "MMOSkillTreeGainXp";

    private GainXpFeature() {}

    /**
     * Registers the type and the XP listener feeding it.
     */
    public static void register(@Nonnull JavaPlugin plugin) {
        QuestProgressionService.get()
                               .registerQuestType(TYPE_ID, MMOSkillTreeGainXpQuestAsset.class, MMOSkillTreeGainXpQuestAsset.CODEC, GainXpQuestProgression.class, GainXpQuestProgression.CODEC);

        MMOSkillTreeAPI.addXpListener(GainXpEvents.XP_LISTENER);
    }

    /**
     * MMO Skill Tree's listeners outlive a plugin that shuts down, so this one is taken back by hand.
     */
    public static void unregister() {
        MMOSkillTreeAPI.removeXpListener(GainXpEvents.XP_LISTENER);
    }
}
