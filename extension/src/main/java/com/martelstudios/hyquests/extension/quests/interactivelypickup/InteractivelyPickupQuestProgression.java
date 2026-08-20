package com.martelstudios.hyquests.extension.quests.interactivelypickup;

import com.hypixel.hytale.builtin.adventure.objectives.config.task.BlockTagOrItemIdField;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.hyquests.extension.quests.count.CountQuestProgression;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class InteractivelyPickupQuestProgression extends CountQuestProgression<InteractivelyPickupQuestProgression> {

    public static final BuilderCodec<InteractivelyPickupQuestProgression> CODEC = BuilderCodec.builder(InteractivelyPickupQuestProgression.class, InteractivelyPickupQuestProgression::new, CountQuestProgression.BASE_CODEC)
                                                                                              .append(new KeyedCodec<>("ItemToPickup", BlockTagOrItemIdField.CODEC), (quest, item) -> quest.itemToPickup = item, quest -> quest.itemToPickup)
                                                                                              .add()
                                                                                              .build();

    @Nullable
    protected BlockTagOrItemIdField itemToPickup;

    @Override
    public InteractivelyPickupQuestAsset getAsset() {
        return (InteractivelyPickupQuestAsset) super.getAsset();
    }

    /**
     * @return this instance's item if one was set on it, the asset's otherwise.
     */
    @Nonnull
    public BlockTagOrItemIdField getItemToPickup() {
        return itemToPickup != null ? itemToPickup : getAsset().getItemToPickup();
    }

    public InteractivelyPickupQuestProgression setItemToPickup(@Nullable BlockTagOrItemIdField itemToPickup) {
        this.itemToPickup = itemToPickup;
        return this;
    }
}
