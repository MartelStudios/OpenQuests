package com.martelstudios.opendialogues.api;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.lookup.CodecMapCodec;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.martelstudios.opendialogues.services.DialogueService;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;

/**
 * What an answer does once picked. Each kind registers under a {@code "Type"} tag through
 * {@link DialogueRegistry#registerAction}, so another mod can add its own.
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

    /**
     * The Node Editor crashes on a dialogue field holding an id missing from its list, so every id
     * an action names is listed, even one no pack declares.
     *
     * @return the ids of the dialogues this action names.
     */
    @Nonnull
    public Collection<String> getReferencedDialogues() {
        return List.of();
    }
}
