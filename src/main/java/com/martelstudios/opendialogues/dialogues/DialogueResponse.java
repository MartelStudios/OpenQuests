package com.martelstudios.opendialogues.dialogues;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.martelstudios.opendialogues.api.DialogueAction;

import javax.annotation.Nonnull;

/**
 * An answer the player can pick, and what picking it does, in order.
 */
public class DialogueResponse {

    public static final BuilderCodec<DialogueResponse> CODEC = BuilderCodec.builder(DialogueResponse.class, DialogueResponse::new)
                                                                           .append(new KeyedCodec<>("Text", Codec.STRING), (response, text) -> response.text = text, response -> response.text)
                                                                           .add()
                                                                           .append(new KeyedCodec<>("Actions", new ArrayCodec<>(DialogueAction.CODEC, DialogueAction[]::new)), (response, actions) -> response.actions = actions, response -> response.actions)
                                                                           .add()
                                                                           .build();

    private String text = "";
    private DialogueAction[] actions = new DialogueAction[0];

    protected DialogueResponse() {}

    /**
     * @return the text of the answer.
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
