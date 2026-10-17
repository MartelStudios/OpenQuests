package com.martelstudios.openquests.mmoskilltree;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.martelstudios.openquests.mmoskilltree.quests.reachlevel.ReachLevelFeature;
import com.martelstudios.openquests.mmoskilltree.rewards.kind.KindRewardFeature;
import com.martelstudios.openquests.mmoskilltree.rewards.xp.XpRewardFeature;

import javax.annotation.Nonnull;

/**
 * OpenQuests quest and reward types built on MMO Skill Tree. Like OpenQuests itself, each type is a
 * feature registering itself, so this class only lists them.
 */
public class OpenQuestsMMOSkillTreePlugin extends JavaPlugin {

    public OpenQuestsMMOSkillTreePlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        super.setup();

        // Quests
        ReachLevelFeature.register(this);

        // Rewards
        XpRewardFeature.register(this);
        KindRewardFeature.register(this);
    }

    @Override
    protected void shutdown() {
        ReachLevelFeature.unregister();

        super.shutdown();
    }
}
