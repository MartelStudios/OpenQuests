package com.martelstudios.opendialogues.actions;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.martelstudios.opendialogues.api.DialogueAction;
import com.martelstudios.opendialogues.services.DialogueService;

import javax.annotation.Nonnull;

/**
 * Moves on to another dialogue, the way {@code /odialogues open} would.
 */
public class OpenDialogueAction extends DialogueAction {

    public static final String TYPE = "OpenDialoguesOpen";

    public static final BuilderCodec<OpenDialogueAction> CODEC = BuilderCodec.builder(OpenDialogueAction.class, OpenDialogueAction::new, DialogueAction.BASE_CODEC)
                                                                           .append(new KeyedCodec<>("Dialogue", Codec.STRING), (action, dialogue) -> action.dialogue = dialogue, action -> action.dialogue)
                                                                           .addValidator(Validators.nonNull())
                                                                           .add()
                                                                           .build();

    private String dialogue;

    @Override
    public void execute(@Nonnull DialogueService dialogues, @Nonnull PlayerRef player) {
        dialogues.open(player, dialogue);
    }
}
