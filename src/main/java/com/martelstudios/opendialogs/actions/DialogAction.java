package com.martelstudios.opendialogs.actions;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.lookup.CodecMapCodec;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.martelstudios.opendialogs.services.DialogService;

import javax.annotation.Nonnull;

/**
 * What an answer does once picked. Each kind registers under a {@code "Type"} tag, so another mod
 * can add its own, completing a quest for instance.
 */
public abstract class DialogAction {

    /**
     * Polymorphic dispatcher over every registered kind.
     */
    public static final CodecMapCodec<DialogAction> CODEC = new CodecMapCodec<>("Type");

    /**
     * Serializes the fields shared by every kind; concrete codecs chain from this.
     */
    public static final BuilderCodec<DialogAction> BASE_CODEC = BuilderCodec.abstractBuilder(DialogAction.class).build();

    /**
     * Runs the action for the player who picked the answer.
     */
    public abstract void execute(@Nonnull DialogService dialogs, @Nonnull PlayerRef player);
}
