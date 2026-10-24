package com.martelstudios.opendialogues.actions.impl.open;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.server.core.Message;
import com.martelstudios.opendialogues.actions.DialogueAction;
import com.martelstudios.opendialogues.api.DialogueContext;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;

/**
 * Moves the conversation on: the dialogue answered closes and the one named opens in its place.
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
    public void execute(@Nonnull DialogueContext context) {
        context.dialogues().close(context.player(), context.session().dialogueId());
        if (!context.dialogues().open(context.player(), dialogue)) {
            context.player().sendMessage(Message.raw("No dialogue '" + dialogue + "'."));
        }
    }

    @Nonnull
    @Override
    public Collection<String> getReferencedDialogues() {
        return List.of(dialogue);
    }
}
