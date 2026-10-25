package com.martelstudios.opendialogues.actions.impl.open;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.Message;
import com.martelstudios.opendialogues.actions.DialogueAction;
import com.martelstudios.opendialogues.api.DialogueContext;
import com.martelstudios.opendialogues.dialogues.OpenDialogueAsset;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

/**
 * Moves the conversation on: the dialogue answered closes and the next one opens in its place,
 * either written inline or named by id.
 */
public class OpenDialogueAction extends DialogueAction {

    public static final String TYPE = "OpenDialoguesOpen";

    public static final BuilderCodec<OpenDialogueAction> CODEC = BuilderCodec.builder(OpenDialogueAction.class, OpenDialogueAction::new, DialogueAction.BASE_CODEC)
                                                                             .append(new KeyedCodec<>("Dialogue", Codec.STRING), (action, dialogue) -> action.dialogue = dialogue, action -> action.dialogue)
                                                                             .add()
                                                                             .append(new KeyedCodec<>("Inline", OpenDialogueAsset.BODY_CODEC), (action, inline) -> action.inline = inline, action -> action.inline)
                                                                             .add()
                                                                             .build();

    @Nullable
    private String dialogue;

    /**
     * Wins over {@link #dialogue} when both are set.
     */
    @Nullable
    private OpenDialogueAsset inline;

    @Override
    public void execute(@Nonnull DialogueContext context) {
        context.dialogues().close(context.player(), context.session().dialogueId());
        if (inline != null) {
            context.dialogues().open(context.player(), inline);
        } else if (dialogue == null || !context.dialogues().open(context.player(), dialogue)) {
            context.player().sendMessage(Message.raw("No dialogue '" + dialogue + "'."));
        }
    }

    @Nonnull
    @Override
    public Collection<OpenDialogueAsset> getInlineDialogues() {
        return inline == null ? List.of() : List.of(inline);
    }
}
