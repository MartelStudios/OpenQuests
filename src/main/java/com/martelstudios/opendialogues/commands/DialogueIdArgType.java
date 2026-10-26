package com.martelstudios.opendialogues.commands;

import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.ParseResult;
import com.hypixel.hytale.server.core.command.system.arguments.types.SingleArgumentType;
import com.hypixel.hytale.server.core.command.system.suggestion.SuggestionResult;
import com.martelstudios.opendialogues.dialogues.OpenDialogueAsset;
import com.martelstudios.opendialogues.routers.OpenDialogueRouterAsset;

import javax.annotation.Nonnull;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

/**
 * The id of a dialogue or a router. The game client asks suggestions of an argument type, never of
 * one argument, so they live here rather than in a provider on the argument.
 */
public class DialogueIdArgType extends SingleArgumentType<String> {

    public DialogueIdArgType() {
        super("Dialogue or router", "Id of a dialogue or a router");
    }

    @Override
    public String parse(String input, ParseResult parseResult) {
        return input;
    }

    /**
     * Dialogues first, then routers no dialogue shadows, each read from its store so a reload is seen.
     */
    @Override
    public void suggest(@Nonnull CommandSender sender, @Nonnull String textAlreadyEntered, int numParametersTyped, @Nonnull SuggestionResult result) {
        String query = textAlreadyEntered.toLowerCase(Locale.ROOT);
        Set<String> dialogueIds = OpenDialogueAsset.getAssetStore().getAssetMap().getAssetMap().keySet();
        Stream.concat(dialogueIds.stream().sorted(),
                      OpenDialogueRouterAsset.getAssetStore().getAssetMap().getAssetMap().keySet().stream().filter(id -> !dialogueIds.contains(id)).sorted())
              .filter(id -> id.toLowerCase(Locale.ROOT).contains(query))
              .takeWhile(id -> !result.isFull())
              .forEach(result::suggest);
    }
}
