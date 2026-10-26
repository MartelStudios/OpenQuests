package com.martelstudios.opendialogues.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.permissions.provider.HytalePermissionsProvider;
import com.hypixel.hytale.server.core.util.Config;
import com.martelstudios.opendialogues.api.DialogueRegistry;
import com.martelstudios.opendialogues.dialogues.OpenDialogueAsset;
import com.martelstudios.opendialogues.displays.DisplayRule;
import com.martelstudios.opendialogues.displays.DisplayRules;
import com.martelstudios.opendialogues.services.DialogueService;

import javax.annotation.Nonnull;
import java.util.List;

import static com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes.INTEGER;
import static com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes.STRING;

/**
 * {@code /odialogues display}: lets the server owner choose which display draws which dialogues.
 * Every change is saved at once.
 */
public class DisplayCommand extends AbstractCommandCollection {

    public DisplayCommand(@Nonnull DialogueService dialogues, @Nonnull DialogueRegistry registry, @Nonnull Config<DisplayRules> rules) {
        super("display", "Chooses which display draws which dialogues");
        setPermissionGroups(HytalePermissionsProvider.GROUP_ADMIN);
        ChoiceArgType displays = new ChoiceArgType("Display", "Id of a registered display", sender -> registry.getDisplayIds().stream());

        addSubCommand(new DefaultCommand(registry, rules, displays));
        addSubCommand(new SetCommand(registry, rules, displays));
        addSubCommand(new UnsetCommand(rules));
        addSubCommand(new ListCommand(rules));
        addSubCommand(new WhichCommand(dialogues));
    }

    private static void warnIfUnknown(@Nonnull CommandContext context, @Nonnull DialogueRegistry registry, @Nonnull String display) {
        if (registry.getDisplay(display) == null) {
            context.sendMessage(Message.raw("No display '" + display + "' is registered yet: dialogues skip this choice until a mod registers it."));
        }
    }

    private static class DefaultCommand extends CommandBase {

        @Nonnull
        private final RequiredArg<String> displayArg;

        @Nonnull
        private final DialogueRegistry registry;

        @Nonnull
        private final Config<DisplayRules> rules;

        DefaultCommand(@Nonnull DialogueRegistry registry, @Nonnull Config<DisplayRules> rules, @Nonnull ChoiceArgType displays) {
            super("default", "Sets the display for dialogues naming none");
            this.registry = registry;
            this.rules = rules;
            this.displayArg = withRequiredArg("display", "Id of the display", displays);
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            String display = context.get(displayArg);
            rules.get().setDefault(display);
            rules.save();
            warnIfUnknown(context, registry, display);
            context.sendMessage(Message.raw("Dialogues naming no display now open in '" + display + "'."));
        }
    }

    private static class SetCommand extends CommandBase {

        @Nonnull
        private final RequiredArg<String> targetArg = withRequiredArg("target", "*, dialogue:<id>, tag:<tag> or pack:<pack>", STRING);

        @Nonnull
        private final RequiredArg<String> displayArg;

        @Nonnull
        private final OptionalArg<Integer> positionArg = withOptionalArg("position", "Place in the rule list, from 1, deciding which tag rule wins", INTEGER);

        @Nonnull
        private final DialogueRegistry registry;

        @Nonnull
        private final Config<DisplayRules> rules;

        SetCommand(@Nonnull DialogueRegistry registry, @Nonnull Config<DisplayRules> rules, @Nonnull ChoiceArgType displays) {
            super("set", "Picks the display for some dialogues, over their own choice");
            this.registry = registry;
            this.rules = rules;
            this.displayArg = withRequiredArg("display", "Id of the display", displays);
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            String target = context.get(targetArg);
            if (!DisplayRule.isTarget(target)) {
                context.sendMessage(Message.raw("Target '" + target + "' is none of *, dialogue:<id>, tag:<tag>, pack:<pack>."));
                return;
            }
            String display = context.get(displayArg);
            rules.get().set(target, display, context.get(positionArg));
            rules.save();
            warnIfUnknown(context, registry, display);
            context.sendMessage(Message.raw("Dialogues matching '" + target + "' now open in '" + display + "'."));
        }
    }

    private static class UnsetCommand extends CommandBase {

        @Nonnull
        private final RequiredArg<String> targetArg;

        @Nonnull
        private final Config<DisplayRules> rules;

        UnsetCommand(@Nonnull Config<DisplayRules> rules) {
            super("unset", "Removes the rule on a target");
            this.rules = rules;
            this.targetArg = withRequiredArg("target", "Target of the rule to remove", new ChoiceArgType("Rule target", "Target of a display rule", sender -> rules.get().getRules().stream().map(DisplayRule::getTarget)));
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            String target = context.get(targetArg);
            if (!rules.get().remove(target)) {
                context.sendMessage(Message.raw("No rule on '" + target + "'."));
                return;
            }
            rules.save();
            context.sendMessage(Message.raw("Rule on '" + target + "' removed."));
        }
    }

    private static class ListCommand extends CommandBase {

        @Nonnull
        private final Config<DisplayRules> rules;

        ListCommand(@Nonnull Config<DisplayRules> rules) {
            super("list", "Shows the display rules in the order they are tried");
            this.rules = rules;
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            List<DisplayRule> list = rules.get().getRules();
            StringBuilder text = new StringBuilder("Default: ").append(rules.get().getDefault());
            for (int i = 0; i < list.size(); i++) {
                text.append('\n').append(i + 1).append(". ").append(list.get(i).getTarget()).append(" uses ").append(list.get(i).getDisplay());
            }
            context.sendMessage(Message.raw(text.toString()));
        }
    }

    private static class WhichCommand extends CommandBase {

        @Nonnull
        private final RequiredArg<String> dialogueArg = withRequiredArg("dialogue", "Id of the dialogue", STRING)
            .withSuggestionOverride(DialogueCommand.DIALOGUE_ASSET);

        @Nonnull
        private final DialogueService dialogues;

        WhichCommand(@Nonnull DialogueService dialogues) {
            super("which", "Tells which display a dialogue opens in, and why");
            this.dialogues = dialogues;
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            String dialogueId = context.get(dialogueArg);
            OpenDialogueAsset dialogue = OpenDialogueAsset.getDialogue(dialogueId);
            if (dialogue == null) {
                context.sendMessage(Message.raw("No dialogue '" + dialogueId + "'."));
                return;
            }
            DisplayRules.Choice choice = dialogues.chooseDisplay(dialogue);
            context.sendMessage(Message.raw("'" + dialogueId + "' opens in '" + choice.display() + "', from " + choice.source()
                                            + ". Pack: " + dialogue.getPack() + ", tags: " + dialogue.getTags() + "."));
        }
    }
}
