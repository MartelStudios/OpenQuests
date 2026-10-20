package com.martelstudios.openquests.mmoskilltree.quests.moment.gainxp;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.OpenQuestsServices;
import com.martelstudios.openquests.mmoskilltree.quests.moment.Moments;
import com.ziggfreed.mmoskilltree.api.MMOSkillTreeAPI;

import javax.annotation.Nonnull;

/**
 * Gain an amount of XP, in one skill or in any.
 */
public final class GainXpFeature {

    /**
     * The listener added, taken back on shutdown: MMO Skill Tree keeps its listeners by identity.
     */
    private static GainXpEvents events;
    public static final String TYPE_ID = "MMOSkillTreeGainXp";

    private GainXpFeature() {}

    /**
     * Registers the type and the XP listener feeding it.
     */
    public static void register(@Nonnull JavaPlugin plugin, @Nonnull OpenQuestsServices services) {
        services.getQuestProgressionService().registerQuestType(TYPE_ID, MMOSkillTreeGainXpQuestAsset.class, MMOSkillTreeGainXpQuestAsset.CODEC, GainXpQuestProgression.class, GainXpQuestProgression.CODEC);

        events = new GainXpEvents(new Moments(services.getQuestProgressionService()));
        MMOSkillTreeAPI.addXpListener(events.xpListener());
    }

    /**
     * MMO Skill Tree's listeners outlive a plugin that shuts down, so this one is taken back by hand.
     */
    public static void unregister() {
        if (events != null) MMOSkillTreeAPI.removeXpListener(events.xpListener());
    }
}
