package com.martelstudios.opendialogues.commands;

import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.ParseResult;
import com.hypixel.hytale.server.core.command.system.arguments.types.SingleArgumentType;
import com.hypixel.hytale.server.core.command.system.suggestion.SuggestionResult;
import com.martelstudios.opendialogues.services.DialogueService;

import javax.annotation.Nonnull;
import java.util.Locale;

/**
 * A dialogue the sender has open. Suggests those only, so answering never offers a dialogue the
 * player cannot answer.
 */
public class OpenDialogueArgType extends SingleArgumentType<String> {

    @Nonnull
    private final DialogueService dialogues;

    public OpenDialogueArgType(@Nonnull DialogueService dialogues) {
        super("Open dialogue", "Id of one of your open dialogues");
        this.dialogues = dialogues;
    }

    @Override
    public String parse(String input, ParseResult parseResult) {
        return input;
    }

    @Override
    public void suggest(@Nonnull CommandSender sender, @Nonnull String textAlreadyEntered, int numParametersTyped, @Nonnull SuggestionResult result) {
        String typed = textAlreadyEntered.toLowerCase(Locale.ROOT);
        dialogues.openDialogues(sender.getUuid()).stream()
                 .filter(id -> id.toLowerCase(Locale.ROOT).startsWith(typed))
                 .forEach(result::suggest);
    }
}
