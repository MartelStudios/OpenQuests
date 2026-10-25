package com.martelstudios.opendialogues.dialogues;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.martelstudios.opendialogues.actions.DialogueAction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * An answer the player can pick, and what picking it does, in order.
 */
public class DialogueResponse {

    public static final BuilderCodec<DialogueResponse> CODEC = BuilderCodec.builder(DialogueResponse.class, DialogueResponse::new)
                                                                           .append(new KeyedCodec<>("Id", Codec.STRING), (response, id) -> response.id = id, response -> response.id)
                                                                           .add()
                                                                           .append(new KeyedCodec<>("Text", Codec.STRING), (response, text) -> response.text = text, response -> response.text)
                                                                           .add()
                                                                           .append(new KeyedCodec<>("Actions", new ArrayCodec<>(DialogueAction.CODEC, DialogueAction[]::new)), (response, actions) -> response.actions = actions, response -> response.actions)
                                                                           .add()
                                                                           .build();

    @Nullable
    private String id;

    private String text = "";
    private DialogueAction[] actions = new DialogueAction[0];

    protected DialogueResponse() {}

    /**
     * @return the name history records the answer under, or {@code null} to record it by position.
     */
    @Nullable
    public String getId() {
        return id;
    }

    /**
     * @return the text of the answer, or its translation key.
     */
    @Nonnull
    public String getText() {
        return text;
    }

    /**
     * @return the actions run when the answer is picked, in order.
     */
    @Nonnull
    public DialogueAction[] getActions() {
        return actions;
    }
}
