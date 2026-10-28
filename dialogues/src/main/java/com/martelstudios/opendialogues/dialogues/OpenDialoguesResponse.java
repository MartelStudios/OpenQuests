package com.martelstudios.opendialogues.dialogues;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.martelstudios.opendialogues.actions.DialogueAction;
import com.martelstudios.opendialogues.conditions.ConditionContext;
import com.martelstudios.opendialogues.conditions.DialogueCondition;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * An answer the player can pick, and what picking it does, in order.
 */
public class OpenDialoguesResponse {

    public static final BuilderCodec<OpenDialoguesResponse> CODEC = BuilderCodec.builder(OpenDialoguesResponse.class, OpenDialoguesResponse::new)
                                                                           .append(new KeyedCodec<>("Id", Codec.STRING), (response, id) -> response.id = id, response -> response.id)
                                                                           .add()
                                                                           .append(new KeyedCodec<>("Text", Codec.STRING), (response, text) -> response.text = text, response -> response.text)
                                                                           .add()
                                                                           .append(new KeyedCodec<>("Actions", new ArrayCodec<>(DialogueAction.CODEC, DialogueAction[]::new)), (response, actions) -> response.actions = actions, response -> response.actions)
                                                                           .add()
                                                                           .append(new KeyedCodec<>("When", DialogueCondition.CODEC), (response, when) -> response.when = when, response -> response.when)
                                                                           .add()
                                                                           .build();

    @Nullable
    private String id;

    private String text = "";
    private DialogueAction[] actions = new DialogueAction[0];

    @Nullable
    private DialogueCondition when;

    protected OpenDialoguesResponse() {}

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

    /**
     * @return the condition the answer is shown under, or {@code null} to always show it.
     */
    @Nullable
    public DialogueCondition getWhen() {
        return when;
    }

    /**
     * @return whether the player may see and pick the answer now.
     */
    public boolean isShownTo(@Nonnull ConditionContext context) {
        return when == null || when.test(context);
    }
}
