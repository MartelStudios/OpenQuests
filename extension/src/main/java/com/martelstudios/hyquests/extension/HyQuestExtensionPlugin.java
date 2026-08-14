package com.martelstudios.hyquests.extension;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.martelstudios.hyquests.extension.gather.GatherFeature;
import com.martelstudios.hyquests.extension.general.GeneralFeature;
import com.martelstudios.hyquests.extension.hud.HudFeature;
import com.martelstudios.hyquests.extension.interactivelypickup.InteractivelyPickupFeature;
import com.martelstudios.hyquests.extension.reachlocation.ReachLocationFeature;
import com.martelstudios.hyquests.extension.rewards.ItemRewardFeature;

import javax.annotation.Nonnull;

/**
 * The quest and reward types shipped on top of HyQuestCore. Each feature registers itself, so
 * this class only lists them — which is also the shape another plugin should copy to add its own.
 */
public class HyQuestExtensionPlugin extends JavaPlugin {

    public HyQuestExtensionPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        super.setup();

        GatherFeature.register(this);
        InteractivelyPickupFeature.register(this);
        ReachLocationFeature.register(this);
        GeneralFeature.register(this);
        HudFeature.register(this);
        ItemRewardFeature.register(this);
    }
}
