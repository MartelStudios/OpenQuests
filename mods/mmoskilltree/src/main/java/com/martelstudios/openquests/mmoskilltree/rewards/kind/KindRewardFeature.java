package com.martelstudios.openquests.mmoskilltree.rewards.kind;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.extension.journal.QuestPageService;

import javax.annotation.Nonnull;

/**
 * The journal line every reward paid through CommonLib shares. Each reward registers itself from
 * its own package.
 */
public final class KindRewardFeature {

    private KindRewardFeature() {}

    /**
     * Registers the line for {@link KindReward} and so for every reward built on it.
     */
    public static void register(@Nonnull JavaPlugin plugin) {
        QuestPageService.register(new KindRewardRenderer());
    }
}
