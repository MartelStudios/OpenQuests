package com.martelstudios.opendialogues.actions;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.martelstudios.opendialogues.services.DialogueService;

import javax.annotation.Nonnull;

/**
 * Runs a command as the player, with the player's own permissions.
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
    public void execute(@Nonnull DialogueService dialogues, @Nonnull PlayerRef player) {
        CommandManager.get().handleCommand(player, command);
    }
}
