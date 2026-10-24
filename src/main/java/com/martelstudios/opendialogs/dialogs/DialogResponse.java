package com.martelstudios.opendialogs.dialogs;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.martelstudios.opendialogs.actions.DialogAction;

import javax.annotation.Nonnull;

/**
 * An answer the player can pick, and what picking it does, in order.
 */
public class DialogResponse {

    public static final BuilderCodec<DialogResponse> CODEC = BuilderCodec.builder(DialogResponse.class, DialogResponse::new)
                                                                         .append(new KeyedCodec<>("Text", Codec.STRING), (response, text) -> response.text = text, response -> response.text)
                                                                         .add()
                                                                         .append(new KeyedCodec<>("Actions", new ArrayCodec<>(DialogAction.CODEC, DialogAction[]::new)), (response, actions) -> response.actions = actions, response -> response.actions)
                                                                         .add()
                                                                         .build();

    private String text = "";
    private DialogAction[] actions = new DialogAction[0];

    protected DialogResponse() {}

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
    public DialogAction[] getActions() {
        return actions;
    }
}
