package com.martelstudios.opendialogs.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.permissions.provider.HytalePermissionsProvider;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.opendialogs.services.DialogService;

import javax.annotation.Nonnull;

import static com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes.INTEGER;
import static com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes.STRING;

/**
 * {@code /odialogs}: opens a dialog by id and picks its answers.
 */
public class DialogCommand extends AbstractCommandCollection {

    public DialogCommand(@Nonnull DialogService dialogs) {
        super("odialogs", "Opens dialogs");
        addAliases("dialogs");

        setPermissionGroups(HytalePermissionsProvider.GROUP_ADVENTURER);
        addSubCommand(new OpenCommand(dialogs));
        addSubCommand(new AnswerCommand(dialogs));
    }

    private static class OpenCommand extends AbstractPlayerCommand {

        @Nonnull
        private final RequiredArg<String> dialogArg = withRequiredArg("dialog", "Id of the dialog to open", STRING);

        @Nonnull
        private final DialogService dialogs;

        OpenCommand(@Nonnull DialogService dialogs) {
            super("open", "Opens a dialog by id");
            setPermissionGroups(HytalePermissionsProvider.GROUP_WORLD_EDITOR);
            this.dialogs = dialogs;
        }

        @Override
        protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
            dialogs.open(playerRef, context.get(dialogArg));
        }
    }

    private static class AnswerCommand extends AbstractPlayerCommand {

        @Nonnull
        private final RequiredArg<Integer> answerArg = withRequiredArg("answer", "Position of the answer, from 1", INTEGER);

        @Nonnull
        private final DialogService dialogs;

        AnswerCommand(@Nonnull DialogService dialogs) {
            super("answer", "Picks an answer of your open dialog");
            this.dialogs = dialogs;
        }

        @Override
        protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
            if (!dialogs.answer(playerRef, context.get(answerArg))) {
                context.sendMessage(Message.raw("No such answer in an open dialog."));
            }
        }
    }
}
