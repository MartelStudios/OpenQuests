package com.martelstudios.opendialogues.conditions.impl.items;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.martelstudios.opendialogues.conditions.CountBounds;
import com.martelstudios.opendialogues.conditions.ConditionContext;
import com.martelstudios.opendialogues.conditions.DialogueCondition;
import com.martelstudios.opendialogues.dialogues.OpenDialogueAsset;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Function;

/**
 * Holds while the player carries between {@code Min} and {@code Max} items the filter accepts,
 * anywhere in their inventory.
 */
public class InventoryCondition extends DialogueCondition {

    public static final String TYPE = "Inventory";

    public static final BuilderCodec<InventoryCondition> CODEC = BuilderCodec.builder(InventoryCondition.class, InventoryCondition::new, DialogueCondition.BASE_CODEC)
                                                                             .append(new KeyedCodec<>("Item", DialogueItemFilter.CODEC, true), (condition, item) -> condition.item = item, condition -> condition.item)
                                                                             .add()
                                                                             .append(new KeyedCodec<>("Min", Codec.INTEGER), (condition, min) -> condition.min = min, condition -> condition.min)
                                                                             .add()
                                                                             .append(new KeyedCodec<>("Max", Codec.INTEGER), (condition, max) -> condition.max = max, condition -> condition.max)
                                                                             .add()
                                                                             .build();

    protected DialogueItemFilter item;
    protected int min = 1;
    @Nullable
    protected Integer max;

    protected InventoryCondition() {}

    /**
     * Fails off the player's world thread, where the inventory cannot be read.
     */
    @Override
    protected boolean evaluate(@Nonnull ConditionContext context) {
        if (!context.canReadEntity()) return false;

        int count = InventoryComponent.getCombined(context.store(), context.entity(), InventoryComponent.HOTBAR_FIRST)
                                      .countItemStacks(stack -> item.matches(stack.getItemId()));
        return CountBounds.within(count, min, max);
    }

    @Nullable
    @Override
    public String validate(@Nonnull Function<String, OpenDialogueAsset> dialogues) {
        return CountBounds.validate(min, max);
    }
}
