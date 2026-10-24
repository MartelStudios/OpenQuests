package com.martelstudios.opendialogs.actions;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.martelstudios.opendialogs.services.DialogService;

import javax.annotation.Nonnull;

/**
 * Runs a command as the player, with the player's own permissions.
 */
public class CommandDialogAction extends DialogAction {

    public static final String TYPE = "OpenDialogsCommand";

    public static final BuilderCodec<CommandDialogAction> CODEC = BuilderCodec.builder(CommandDialogAction.class, CommandDialogAction::new, DialogAction.BASE_CODEC)
                                                                              .append(new KeyedCodec<>("Command", Codec.STRING), (action, command) -> action.command = command, action -> action.command)
                                                                              .addValidator(Validators.nonNull())
                                                                              .add()
                                                                              .build();

    private String command;

    @Override
    public void execute(@Nonnull DialogService dialogs, @Nonnull PlayerRef player) {
        CommandManager.get().handleCommand(player, command);
    }
}
