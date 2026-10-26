package com.martelstudios.opendialogues.conditions.impl.items;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.martelstudios.opendialogues.conditions.ConditionContext;
import com.martelstudios.opendialogues.conditions.DialogueCondition;

import javax.annotation.Nonnull;

/**
 * Holds while the player carries at least {@code Min} items the filter accepts, anywhere in their
 * inventory.
 */
public class InventoryCondition extends DialogueCondition {

    public static final String TYPE = "Inventory";

    public static final BuilderCodec<InventoryCondition> CODEC = BuilderCodec.builder(InventoryCondition.class, InventoryCondition::new, DialogueCondition.BASE_CODEC)
                                                                             .append(new KeyedCodec<>("Item", DialogueItemFilter.CODEC, true), (condition, item) -> condition.item = item, condition -> condition.item)
                                                                             .add()
                                                                             .append(new KeyedCodec<>("Min", Codec.INTEGER), (condition, min) -> condition.min = min, condition -> condition.min)
                                                                             .add()
                                                                             .build();

    protected DialogueItemFilter item;
    protected int min = 1;

    protected InventoryCondition() {}

    /**
     * Fails off the player's world thread, where the inventory cannot be read.
     */
    @Override
    protected boolean evaluate(@Nonnull ConditionContext context) {
        if (!context.canReadEntity()) return false;

        return InventoryComponent.getCombined(context.store(), context.entity(), InventoryComponent.HOTBAR_FIRST)
                                 .countItemStacks(stack -> item.matches(stack.getItemId())) >= min;
    }
}
