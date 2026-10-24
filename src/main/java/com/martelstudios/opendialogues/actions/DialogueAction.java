package com.martelstudios.opendialogues.actions;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.lookup.CodecMapCodec;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.martelstudios.opendialogues.services.DialogueService;

import javax.annotation.Nonnull;

/**
 * What an answer does once picked. Each kind registers under a {@code "Type"} tag, so another mod
 * can add its own, completing a quest for instance.
 */
public abstract class DialogueAction {

    /**
     * Polymorphic dispatcher over every registered kind.
     */
    public static final CodecMapCodec<DialogueAction> CODEC = new CodecMapCodec<>("Type");

    /**
     * Serializes the fields shared by every kind; concrete codecs chain from this.
     */
    public static final BuilderCodec<DialogueAction> BASE_CODEC = BuilderCodec.abstractBuilder(DialogueAction.class).build();

    /**
     * Runs the action for the player who picked the answer.
     */
    public abstract void execute(@Nonnull DialogueService dialogues, @Nonnull PlayerRef player);
}
