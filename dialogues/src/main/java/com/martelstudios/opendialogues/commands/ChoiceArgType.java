package com.martelstudios.opendialogues.commands;

import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.ParseResult;
import com.hypixel.hytale.server.core.command.system.arguments.types.SingleArgumentType;
import com.hypixel.hytale.server.core.command.system.suggestion.SuggestionResult;

import javax.annotation.Nonnull;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * A word suggested from a list read on each request. The game client asks suggestions of an
 * argument type, never of one argument, so a provider set on a {@code STRING} argument only reaches the console.
 */
public class ChoiceArgType extends SingleArgumentType<String> {

    @Nonnull
    private final Function<CommandSender, Stream<String>> choices;

    /**
     * @param choices the words to suggest that sender, in the order shown.
     */
    public ChoiceArgType(@Nonnull String name, @Nonnull String usage, @Nonnull Function<CommandSender, Stream<String>> choices) {
        super(name, usage);
        this.choices = choices;
    }

    @Override
    public String parse(String input, ParseResult parseResult) {
        return input;
    }

    @Override
    public void suggest(@Nonnull CommandSender sender, @Nonnull String textAlreadyEntered, int numParametersTyped, @Nonnull SuggestionResult result) {
        String query = textAlreadyEntered.toLowerCase(Locale.ROOT);
        choices.apply(sender)
               .filter(choice -> choice.toLowerCase(Locale.ROOT).contains(query))
               .takeWhile(choice -> !result.isFull())
               .forEach(result::suggest);
    }
}
