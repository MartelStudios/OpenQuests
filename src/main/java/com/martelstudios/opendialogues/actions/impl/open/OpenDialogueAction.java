package com.martelstudios.opendialogues.actions.impl.open;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.Message;
import com.martelstudios.opendialogues.actions.DialogueAction;
import com.martelstudios.opendialogues.api.DialogueContext;
import com.martelstudios.opendialogues.dialogues.OpenDialogueAsset;
import com.martelstudios.opendialogues.routers.DialogueTarget;
import com.martelstudios.opendialogues.routers.OpenDialogueRouterAsset;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Moves the conversation on: the dialogue answered closes and the next one opens in its place,
 * a dialogue or the one a router picks, either written inline or named by id.
 */
public class OpenDialogueAction extends DialogueAction {

    public static final String TYPE = "OpenDialoguesOpen";

    public static final BuilderCodec<OpenDialogueAction> CODEC = BuilderCodec.builder(OpenDialogueAction.class, OpenDialogueAction::new, DialogueAction.BASE_CODEC)
                                                                             .append(new KeyedCodec<>("Dialogue", OpenDialogueAsset.CONTAINED_CODEC), (action, dialogue) -> action.dialogue = dialogue, action -> action.dialogue)
                                                                             .add()
                                                                             .append(new KeyedCodec<>("Router", OpenDialogueRouterAsset.CONTAINED_CODEC), (action, router) -> action.router = router, action -> action.router)
                                                                             .add()
                                                                             .validator((action, results) -> {
                                                                                 if ((action.dialogue == null) == (action.router == null)) {
                                                                                     results.fail("Exactly one of Dialogue or Router must be set");
                                                                                 }
                                                                             })
                                                                             .build();

    @Nullable
    private String dialogue;

    @Nullable
    private String router;

    @Override
    public void execute(@Nonnull DialogueContext context) {
        context.dialogues().close(context.player(), context.session().dialogueId());
        if (!context.dialogues().open(context.player(), DialogueTarget.of(dialogue, router))) {
            context.player().sendMessage(Message.raw("No dialogue from '" + (dialogue != null ? dialogue : router) + "'."));
        }
    }
}
