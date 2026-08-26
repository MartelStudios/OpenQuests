package com.martelstudios.openquests.extension;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.martelstudios.openquests.extension.quests.gather.GatherFeature;
import com.martelstudios.openquests.extension.quests.composite.CompositeFeature;
import com.martelstudios.openquests.extension.hud.HudFeature;
import com.martelstudios.openquests.extension.quests.interactivelypickup.InteractivelyPickupFeature;
import com.martelstudios.openquests.extension.quests.craft.CraftFeature;
import com.martelstudios.openquests.extension.quests.kill.KillFeature;
import com.martelstudios.openquests.extension.quests.useblock.UseBlockFeature;
import com.martelstudios.openquests.extension.quests.queststate.QuestStateFeature;
import com.martelstudios.openquests.extension.quests.useentity.UseEntityFeature;
import com.martelstudios.openquests.extension.quests.enterworld.EnterWorldFeature;
import com.martelstudios.openquests.extension.quests.reachlocation.ReachLocationFeature;
import com.martelstudios.openquests.extension.rewards.command.CommandRewardFeature;
import com.martelstudios.openquests.extension.rewards.grantquest.GrantQuestRewardFeature;
import com.martelstudios.openquests.extension.rewards.item.ItemRewardFeature;

import javax.annotation.Nonnull;

/**
 * The quest and reward types shipped on top of OpenQuestCore. Each feature registers itself, so
 * this class only lists them — which is also the shape another plugin should copy to add its own.
 */
public class OpenQuestExtensionPlugin extends JavaPlugin {

    public OpenQuestExtensionPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        super.setup();

        // Quests
        GatherFeature.register(this);
        InteractivelyPickupFeature.register(this);
        ReachLocationFeature.register(this);
        EnterWorldFeature.register(this);
        CompositeFeature.register(this);
        HudFeature.register(this);
        KillFeature.register(this);
        CraftFeature.register(this);
        UseBlockFeature.register(this);
        UseEntityFeature.register(this);
        QuestStateFeature.register(this);

        // Rewards
        ItemRewardFeature.register(this);
        GrantQuestRewardFeature.register(this);
        CommandRewardFeature.register(this);
    }
}
