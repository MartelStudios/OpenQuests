package com.martelstudios.opendialogues.actions.impl.command;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.console.ConsoleSender;
import com.martelstudios.opendialogues.actions.DialogueAction;
import com.martelstudios.opendialogues.api.DialogueContext;

import javax.annotation.Nonnull;

/**
 * Runs a command from the console, or as the player with their own permissions when
 * {@code AsPlayer} is set. {@code {player}} stands for the player's username. The dialogue stays open.
 */
public class OpenDialoguesCommandAction extends DialogueAction {

    public static final String TYPE = "Command";

    public static final BuilderCodec<OpenDialoguesCommandAction> CODEC = BuilderCodec.builder(OpenDialoguesCommandAction.class, OpenDialoguesCommandAction::new, DialogueAction.BASE_CODEC)
                                                                                .append(new KeyedCodec<>("Command", Codec.STRING), (action, command) -> action.command = command, action -> action.command)
                                                                                .addValidator(Validators.nonNull())
                                                                                .add()
                                                                                .append(new KeyedCodec<>("AsPlayer", Codec.BOOLEAN), (action, asPlayer) -> action.asPlayer = asPlayer, action -> action.asPlayer)
                                                                                .add()
                                                                                .build();

    private String command;

    private boolean asPlayer;

    @Override
    public void execute(@Nonnull DialogueContext context) {
        CommandSender sender = asPlayer ? context.player() : ConsoleSender.INSTANCE;
        CommandManager.get().handleCommand(sender, resolve(command, context.player().getUsername()));
    }

    /**
     * @return whether the command runs as the player rather than from the console.
     */
    public boolean isAsPlayer() {
        return asPlayer;
    }

    /**
     * @return the command with every {@code {player}} replaced by the username.
     */
    @Nonnull
    public static String resolve(@Nonnull String command, @Nonnull String username) {
        return command.replace("{player}", username);
    }
}
