package com.martelstudios.opendialogues.actions.impl.inventory;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.martelstudios.opendialogues.conditions.impl.items.OpenDialoguesItemFilter;

import javax.annotation.Nonnull;

/**
 * How many items a filter must find, as an entry of a list such as the one an inventory action takes.
 */
public class OpenDialoguesItemQuantity {

    public static final BuilderCodec<OpenDialoguesItemQuantity> CODEC = BuilderCodec.builder(OpenDialoguesItemQuantity.class, OpenDialoguesItemQuantity::new)
                                                                                .append(new KeyedCodec<>("Item", OpenDialoguesItemFilter.CODEC, true), (entry, item) -> entry.item = item, entry -> entry.item)
                                                                                .addValidator(Validators.nonNull())
                                                                                .add()
                                                                                .append(new KeyedCodec<>("Quantity", Codec.INTEGER), (entry, quantity) -> entry.quantity = quantity, entry -> entry.quantity)
                                                                                .addValidator(Validators.min(1))
                                                                                .add()
                                                                                .build();

    protected OpenDialoguesItemFilter item;

    protected int quantity = 1;

    protected OpenDialoguesItemQuantity() {}

    /**
     * @param quantity at least one.
     */
    public OpenDialoguesItemQuantity(@Nonnull OpenDialoguesItemFilter item, int quantity) {
        this.item = item;
        this.quantity = quantity;
    }

    /**
     * @return the items this entry accepts.
     */
    @Nonnull
    public OpenDialoguesItemFilter item() {
        return item;
    }

    /**
     * @return how many of them are needed.
     */
    public int quantity() {
        return quantity;
    }
}
