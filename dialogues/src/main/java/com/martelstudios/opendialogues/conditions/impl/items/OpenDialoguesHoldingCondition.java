package com.martelstudios.opendialogues.conditions.impl.items;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.martelstudios.opendialogues.conditions.ConditionContext;
import com.martelstudios.opendialogues.conditions.DialogueCondition;

import javax.annotation.Nonnull;

/**
 * Holds while the item in the player's hand is one the filter accepts.
 */
public class OpenDialoguesHoldingCondition extends DialogueCondition {

    public static final String TYPE = "Holding";

    public static final BuilderCodec<OpenDialoguesHoldingCondition> CODEC = BuilderCodec.builder(OpenDialoguesHoldingCondition.class, OpenDialoguesHoldingCondition::new, DialogueCondition.BASE_CODEC)
                                                                           .append(new KeyedCodec<>("Item", OpenDialoguesItemFilter.CODEC, true), (condition, item) -> condition.item = item, condition -> condition.item)
                                                                           .add()
                                                                           .build();

    protected OpenDialoguesItemFilter item;

    protected OpenDialoguesHoldingCondition() {}

    /**
     * Fails off the player's world thread, where the hand cannot be read.
     */
    @Override
    protected boolean evaluate(@Nonnull ConditionContext context) {
        if (!context.canReadEntity()) return false;

        ItemStack held = InventoryComponent.getItemInHand(context.store(), context.entity());
        return held != null && !held.isEmpty() && item.matches(held.getItemId());
    }
}
