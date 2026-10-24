package com.martelstudios.opendialogues.commands;

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
import com.martelstudios.opendialogues.services.DialogueService;

import javax.annotation.Nonnull;

import static com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes.INTEGER;
import static com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes.STRING;

/**
 * {@code /odialogues}: opens a dialogue by id and picks its answers.
 */
public class DialogueCommand extends AbstractCommandCollection {

    public DialogueCommand(@Nonnull DialogueService dialogues) {
        super("odialogues", "Opens dialogues");
        addAliases("od");

        setPermissionGroups(HytalePermissionsProvider.GROUP_ADVENTURER);
        addSubCommand(new OpenCommand(dialogues));
        addSubCommand(new AnswerCommand(dialogues));
    }

    private static class OpenCommand extends AbstractPlayerCommand {

        @Nonnull
        private final RequiredArg<String> dialogueArg = withRequiredArg("dialogue", "Id of the dialogue to open", STRING);

        @Nonnull
        private final DialogueService dialogues;

        OpenCommand(@Nonnull DialogueService dialogues) {
            super("open", "Opens a dialogue by id");
            setPermissionGroups(HytalePermissionsProvider.GROUP_WORLD_EDITOR);
            this.dialogues = dialogues;
        }

        @Override
        protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
            dialogues.open(playerRef, context.get(dialogueArg));
        }
    }

    private static class AnswerCommand extends AbstractPlayerCommand {

        @Nonnull
        private final RequiredArg<Integer> answerArg = withRequiredArg("answer", "Position of the answer, from 1", INTEGER);

        @Nonnull
        private final DialogueService dialogues;

        AnswerCommand(@Nonnull DialogueService dialogues) {
            super("answer", "Picks an answer of your open dialogue");
            this.dialogues = dialogues;
        }

        @Override
        protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
            if (!dialogues.answer(playerRef, context.get(answerArg))) {
                context.sendMessage(Message.raw("No such answer in an open dialogue."));
            }
        }
    }
}
