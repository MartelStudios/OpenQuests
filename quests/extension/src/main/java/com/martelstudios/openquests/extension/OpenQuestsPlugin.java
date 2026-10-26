package com.martelstudios.openquests.extension;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.martelstudios.openquests.core.OpenQuestsServices;
import com.martelstudios.openquests.extension.constraints.condition.ConditionConstraintsFeature;
import com.martelstudios.openquests.extension.constraints.death.DeathConstraintsFeature;
import com.martelstudios.openquests.extension.constraints.location.LocationConstraintsFeature;
import com.martelstudios.openquests.extension.constraints.players.PlayersConstraintsFeature;
import com.martelstudios.openquests.extension.constraints.repeat.RepeatConstraintsFeature;
import com.martelstudios.openquests.extension.constraints.time.TimeConstraintsFeature;
import com.martelstudios.openquests.extension.feedback.FeedbackFeature;
import com.martelstudios.openquests.extension.hud.HudFeature;
import com.martelstudios.openquests.extension.journal.JournalFeature;
import com.martelstudios.openquests.extension.listener.ListenerFeature;
import com.martelstudios.openquests.extension.quests.breakblock.BreakBlockFeature;
import com.martelstudios.openquests.extension.quests.composite.CompositeFeature;
import com.martelstudios.openquests.extension.quests.consumeitem.ConsumeItemFeature;
import com.martelstudios.openquests.extension.quests.craft.CraftFeature;
import com.martelstudios.openquests.extension.quests.dropitem.DropItemFeature;
import com.martelstudios.openquests.extension.quests.enterworld.EnterWorldFeature;
import com.martelstudios.openquests.extension.quests.gather.GatherFeature;
import com.martelstudios.openquests.extension.quests.interactivelypickup.InteractivelyPickupFeature;
import com.martelstudios.openquests.extension.quests.kill.KillFeature;
import com.martelstudios.openquests.extension.quests.movement.MovementFeature;
import com.martelstudios.openquests.extension.quests.noop.NoOpFeature;
import com.martelstudios.openquests.extension.quests.pickupitem.PickupItemFeature;
import com.martelstudios.openquests.extension.quests.placeblock.PlaceBlockFeature;
import com.martelstudios.openquests.extension.quests.quantity.QuantityFeature;
import com.martelstudios.openquests.extension.quests.queststate.QuestStateFeature;
import com.martelstudios.openquests.extension.quests.reachlocation.ReachLocationFeature;
import com.martelstudios.openquests.extension.quests.triggervolume.TriggerVolumeFeature;
import com.martelstudios.openquests.extension.quests.useblock.UseBlockFeature;
import com.martelstudios.openquests.extension.quests.useentity.UseEntityFeature;
import com.martelstudios.openquests.extension.rewards.choice.ChoiceRewardFeature;
import com.martelstudios.openquests.extension.rewards.command.CommandRewardFeature;
import com.martelstudios.openquests.extension.rewards.grantquest.GrantQuestRewardFeature;
import com.martelstudios.openquests.extension.rewards.item.ItemRewardFeature;
import com.martelstudios.openquests.extension.rewards.random.RandomRewardFeature;
import com.martelstudios.openquests.extension.track.QuestTrackService;

import javax.annotation.Nonnull;

/**
 * The quest and reward types shipped on top of OpenQuestsCore. Each feature registers itself, so
 * this class only lists them, which is also the shape another plugin should copy to add its own.
 */
public class OpenQuestsPlugin extends JavaPlugin {

    public OpenQuestsPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        super.setup();

        // Set up by OpenQuestsCore, which this plugin depends on and which sets up first
        OpenQuestsServices services = OpenQuestsServices.installed();

        // Shared machinery, before the types that declare themselves to it
        ListenerFeature.register(this, services);
        QuestTrackService tracks = new QuestTrackService(services.getQuestProgressionService(), services.getQuestPlayerStateService(), services.events());

        // Quests
        GatherFeature.register(this, services);
        InteractivelyPickupFeature.register(this, services);
        DropItemFeature.register(this, services);
        PickupItemFeature.register(this, services);
        ReachLocationFeature.register(this, services);
        EnterWorldFeature.register(this, services);
        TriggerVolumeFeature.register(this, services);
        CompositeFeature.register(this, services);
        HudFeature.register(this, services, tracks);
        JournalFeature.register(this, services, tracks);
        FeedbackFeature.register(this, services);
        KillFeature.register(this, services);
        CraftFeature.register(this, services);
        UseBlockFeature.register(this, services);
        BreakBlockFeature.register(this, services);
        PlaceBlockFeature.register(this, services);
        ConsumeItemFeature.register(this, services);
        UseEntityFeature.register(this, services);
        QuestStateFeature.register(this, services);
        MovementFeature.register(this, services);
        QuantityFeature.register(this, services);
        NoOpFeature.register(this, services);

        // Rewards
        ItemRewardFeature.register(this, services);
        GrantQuestRewardFeature.register(this, services);
        CommandRewardFeature.register(this, services);
        RandomRewardFeature.register(this, services);
        ChoiceRewardFeature.register(this, services);

        // Constraints
        TimeConstraintsFeature.register(this, services);
        LocationConstraintsFeature.register(this, services);
        ConditionConstraintsFeature.register(this, services);
        PlayersConstraintsFeature.register(this, services);
        DeathConstraintsFeature.register(this, services);
        RepeatConstraintsFeature.register(this, services);
    }
}
