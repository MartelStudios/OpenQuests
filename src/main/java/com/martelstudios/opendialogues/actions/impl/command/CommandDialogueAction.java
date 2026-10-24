package com.martelstudios.opendialogues.actions.impl.command;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.martelstudios.opendialogues.actions.DialogueAction;
import com.martelstudios.opendialogues.api.DialogueContext;

import javax.annotation.Nonnull;

/**
 * Runs a command as the player, with the player's own permissions. The dialogue stays open.
 */
public class CommandDialogueAction extends DialogueAction {

    public static final String TYPE = "OpenDialoguesCommand";

    public static final BuilderCodec<CommandDialogueAction> CODEC = BuilderCodec.builder(CommandDialogueAction.class, CommandDialogueAction::new, DialogueAction.BASE_CODEC)
                                                                                .append(new KeyedCodec<>("Command", Codec.STRING), (action, command) -> action.command = command, action -> action.command)
                                                                                .addValidator(Validators.nonNull())
                                                                                .add()
                                                                                .build();

    private String command;

    @Override
    public void execute(@Nonnull DialogueContext context) {
        CommandManager.get().handleCommand(context.player(), command);
    }
}
