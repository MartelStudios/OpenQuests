package com.martelstudios.opendialogues.actions.impl.close;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.opendialogues.actions.DialogueAction;
import com.martelstudios.opendialogues.api.DialogueContext;

import javax.annotation.Nonnull;

/**
 * Ends the dialogue answered.
 */
public class OpenDialoguesCloseAction extends DialogueAction {

    public static final String TYPE = "Close";

    public static final BuilderCodec<OpenDialoguesCloseAction> CODEC = BuilderCodec.builder(OpenDialoguesCloseAction.class, OpenDialoguesCloseAction::new, DialogueAction.BASE_CODEC).build();

    @Override
    public void execute(@Nonnull DialogueContext context) {
        context.dialogues().close(context.player(), context.session().dialogueId());
    }
}
