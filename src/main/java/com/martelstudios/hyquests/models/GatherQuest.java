package com.martelstudios.hyquests.models;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.hyquests.assets.GatherQuestAsset;
import com.martelstudios.hyquests.visitors.QuestVisitor;

/**
 * Quest whose progress is however many of a given item the player currently holds, rechecked
 * on every inventory change. Unlike {@link InteractivelyPickupQuest} (which only counts a specific
 * pickup interaction), this doesn't care how the item was obtained — crafted, looted, harvested,
 * traded, picked up off the ground, ... Mirrors Hytale's {@code GatherObjectiveTask}, which is
 * driven by {@code InventoryChangeEvent} and recounts matching items rather than accumulating them.
 */
public class GatherQuest extends CountQuest<GatherQuest> {

    public static final BuilderCodec<GatherQuest> CODEC = BuilderCodec.builder(GatherQuest.class, GatherQuest::new, CountQuest.BASE_CODEC)
                                                                       .build();

    @Override
    public GatherQuestAsset getAsset() {
        return (GatherQuestAsset) super.getAsset();
    }
}
