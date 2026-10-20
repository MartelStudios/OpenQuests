package com.martelstudios.openquests.mmoskilltree;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.martelstudios.openquests.core.OpenQuestsServices;
import com.martelstudios.openquests.mmoskilltree.editor.EditorFeature;
import com.martelstudios.openquests.mmoskilltree.quests.moment.abilitycast.AbilityCastFeature;
import com.martelstudios.openquests.mmoskilltree.quests.moment.completebounty.CompleteBountyFeature;
import com.martelstudios.openquests.mmoskilltree.quests.moment.gainxp.GainXpFeature;
import com.martelstudios.openquests.mmoskilltree.quests.reachlevel.ReachLevelFeature;
import com.martelstudios.openquests.mmoskilltree.rewards.abilitymod.AbilityModRewardFeature;
import com.martelstudios.openquests.mmoskilltree.rewards.boosttoken.BoostTokenRewardFeature;
import com.martelstudios.openquests.mmoskilltree.rewards.costume.CostumeRewardFeature;
import com.martelstudios.openquests.mmoskilltree.rewards.currency.CurrencyRewardFeature;
import com.martelstudios.openquests.mmoskilltree.rewards.droplist.DroplistRewardFeature;
import com.martelstudios.openquests.mmoskilltree.rewards.effect.EffectRewardFeature;
import com.martelstudios.openquests.mmoskilltree.rewards.flair.FlairRewardFeature;
import com.martelstudios.openquests.mmoskilltree.rewards.kind.KindRewardFeature;
import com.martelstudios.openquests.mmoskilltree.rewards.lamp.LampRewardFeature;
import com.martelstudios.openquests.mmoskilltree.rewards.level.LevelRewardFeature;
import com.martelstudios.openquests.mmoskilltree.rewards.lootable.LootableRewardFeature;
import com.martelstudios.openquests.mmoskilltree.rewards.reputation.ReputationRewardFeature;
import com.martelstudios.openquests.mmoskilltree.rewards.title.TitleRewardFeature;
import com.martelstudios.openquests.mmoskilltree.rewards.xp.XpRewardFeature;
import com.martelstudios.openquests.mmoskilltree.skills.SkillsFeature;

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

        // Set up by OpenQuestsCore, which this plugin depends on and which sets up first
        OpenQuestsServices services = OpenQuestsServices.installed();

        // Shared machinery
        EditorFeature.register(this);
        SkillsFeature.register(this);
        KindRewardFeature.register(this);

        // Quests
        ReachLevelFeature.register(this, services);
        GainXpFeature.register(this, services);
        AbilityCastFeature.register(this, services);
        CompleteBountyFeature.register(this, services);

        // Rewards
        XpRewardFeature.register(this);
        LevelRewardFeature.register(this);
        CurrencyRewardFeature.register(this);
        ReputationRewardFeature.register(this);
        LootableRewardFeature.register(this);
        DroplistRewardFeature.register(this);
        EffectRewardFeature.register(this);
        CostumeRewardFeature.register(this);
        TitleRewardFeature.register(this);
        FlairRewardFeature.register(this);
        BoostTokenRewardFeature.register(this);
        LampRewardFeature.register(this);
        AbilityModRewardFeature.register(this);
    }

    @Override
    protected void shutdown() {
        ReachLevelFeature.unregister();
        GainXpFeature.unregister();

        super.shutdown();
    }
}
