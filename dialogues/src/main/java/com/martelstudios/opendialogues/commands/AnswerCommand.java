package com.martelstudios.opendialogues.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.permissions.provider.HytalePermissionsProvider;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.opendialogues.services.DialogueService;

import javax.annotation.Nonnull;

import static com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes.INTEGER;

/**
 * Picks an answer of the sender's open dialogue: {@code /oda <n>}. Serves both {@code /oda} and
 * {@code /odialogues answer}.
 */
public class AnswerCommand extends AbstractPlayerCommand {

    @Nonnull
    private final RequiredArg<Integer> answerArg = withRequiredArg("answer", "Position of the answer, from 1", INTEGER);

    @Nonnull
    private final DialogueService dialogues;

    public AnswerCommand(@Nonnull String name, @Nonnull DialogueService dialogues) {
        super(name, "Picks an answer of an open dialogue");
        setPermissionGroups(HytalePermissionsProvider.GROUP_ADVENTURER);
        this.dialogues = dialogues;
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        switch (dialogues.answer(playerRef, null, context.get(answerArg))) {
            case ANSWERED -> {}
            case NO_DIALOGUE -> context.sendMessage(Message.raw("No dialogue open."));
            case NO_SUCH_ANSWER -> context.sendMessage(Message.raw("No such answer."));
            case REFRESHED -> {}
        }
    }
}
