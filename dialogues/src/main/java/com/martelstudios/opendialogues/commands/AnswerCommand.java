package com.martelstudios.opendialogues.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.permissions.provider.HytalePermissionsProvider;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.opendialogues.displays.impl.chat.ChatDisplay;
import com.martelstudios.opendialogues.services.DialogueService;

import javax.annotation.Nonnull;

import static com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes.INTEGER;

/**
 * Picks an answer of one of the sender's open dialogues: {@code /oda <n> [dialogue]}, the dialogue
 * named only when several are open. Serves both {@code /oda} and {@code /odialogues answer}.
 */
public class AnswerCommand extends AbstractPlayerCommand {

    @Nonnull
    private final RequiredArg<Integer> answerArg = withRequiredArg("answer", "Position of the answer, from 1", INTEGER);

    @Nonnull
    private final OptionalArg<String> dialogueArg;

    @Nonnull
    private final DialogueService dialogues;

    public AnswerCommand(@Nonnull String name, @Nonnull DialogueService dialogues) {
        super(name, "Picks an answer of an open dialogue");
        setPermissionGroups(HytalePermissionsProvider.GROUP_ADVENTURER);
        this.dialogues = dialogues;
        this.dialogueArg = withOptionalArg("dialogue", "The dialogue answered, when several are open", new ChoiceArgType("Open dialogue", "Id of one of your open dialogues", sender -> dialogues.openDialogues(sender.getUuid()).stream()));
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        String dialogueId = context.get(dialogueArg);
        switch (dialogues.answer(playerRef, dialogueId, context.get(answerArg))) {
            case ANSWERED -> {}
            case NO_DIALOGUE -> context.sendMessage(Message.raw(dialogueId == null ? "No dialogue open." : "Dialogue '" + dialogueId + "' is not open."));
            case AMBIGUOUS -> context.sendMessage(ChatDisplay.ambiguous(dialogues.openDialogues(playerRef.getUuid())));
            case NO_SUCH_ANSWER -> context.sendMessage(Message.raw("No such answer."));
        }
    }
}
