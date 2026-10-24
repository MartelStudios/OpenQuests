package com.martelstudios.opendialogues.actions;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.martelstudios.opendialogues.services.DialogueService;

import javax.annotation.Nonnull;

/**
 * Ends the dialogue.
 */
public class CloseDialogueAction extends DialogueAction {

    public static final String TYPE = "OpenDialoguesClose";

    public static final BuilderCodec<CloseDialogueAction> CODEC = BuilderCodec.builder(CloseDialogueAction.class, CloseDialogueAction::new, DialogueAction.BASE_CODEC).build();

    @Override
    public void execute(@Nonnull DialogueService dialogues, @Nonnull PlayerRef player) {
        dialogues.close(player);
    }
}
