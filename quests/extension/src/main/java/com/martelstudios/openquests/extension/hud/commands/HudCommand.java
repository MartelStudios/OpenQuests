package com.martelstudios.openquests.extension.hud.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.permissions.provider.HytalePermissionsProvider;
import com.martelstudios.openquests.core.models.HudSettings;
import com.martelstudios.openquests.extension.hud.QuestHudSettings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * {@code /ohud show|hide|move|reset} changes the sender's own quest tracker, kept in their record.
 * {@code /ohud default …} takes the same words and changes everyone's defaults instead, kept in
 * {@code hud.json}.
 */
public class HudCommand extends AbstractCommandCollection {

    private final QuestHudSettings settings;

    public HudCommand(@Nonnull QuestHudSettings settings) {
        super("ohud", "Shows, hides or moves your quest tracker");
        this.settings = settings;
        setPermissionGroups(HytalePermissionsProvider.GROUP_ADVENTURER);

        addTargetCommands(this, false, HytalePermissionsProvider.GROUP_ADVENTURER);
        addSubCommand(new DefaultsCommand());
    }

    private class DefaultsCommand extends AbstractCommandCollection {
        DefaultsCommand() {
            super("default", "Changes the quest tracker of everyone who chose nothing");
            setPermissionGroups(HytalePermissionsProvider.GROUP_WORLD_EDITOR);
            addTargetCommands(this, true, HytalePermissionsProvider.GROUP_WORLD_EDITOR);
        }
    }

    private void addTargetCommands(@Nonnull AbstractCommandCollection parent, boolean defaults, @Nonnull String permissionGroup) {
        parent.addSubCommand(new VisibilityCommand("show", "Shows the quest tracker", true, defaults, permissionGroup));
        parent.addSubCommand(new VisibilityCommand("hide", "Hides the quest tracker", false, defaults, permissionGroup));
        parent.addSubCommand(new MoveCommand(defaults, permissionGroup));
        parent.addSubCommand(new ResetCommand(defaults, permissionGroup));
    }

    /**
     * Applies the choice to the sender, or to everyone's defaults, then says what the tracker now
     * looks like.
     *
     * @param choice {@code null} drops what was chosen rather than adding to it.
     */
    private void apply(@Nonnull CommandContext context, boolean defaults, @Nullable HudSettings choice) {
        if (defaults) {
            if (choice == null) settings.getDefaults().reset(); else settings.getDefaults().override(choice);

            context.sendMessage(describe("openquests.hud.defaults", settings.getDefaults().get()));
            return;
        }

        if (!context.isPlayer()) {
            context.sendMessage(Message.translation("openquests.hud.players-only"));
            return;
        }

        UUID playerId = context.sender().getUuid();
        boolean applied = choice == null ? settings.reset(playerId) : settings.choose(playerId, choice);
        if (applied) context.sendMessage(describe("openquests.hud.yours", settings.resolve(playerId)));
    }

    /**
     * The console does not format {@code select}, so the words go in as messages of their own.
     */
    @Nonnull
    private static Message describe(@Nonnull String key, @Nonnull HudSettings settings) {
        return Message.translation(key)
                      .param("state", Message.translation(Boolean.TRUE.equals(settings.getVisible()) ? "openquests.hud.shown" : "openquests.hud.hidden"))
                      .param("side", Message.translation(settings.getSide() == HudSettings.Side.LEFT ? "openquests.hud.left" : "openquests.hud.right"))
                      .param("offset", settings.getOffset())
                      .param("top", settings.getTop());
    }

    private class VisibilityCommand extends CommandBase {
        private final boolean visible;
        private final boolean defaults;

        VisibilityCommand(@Nonnull String name, @Nonnull String description, boolean visible, boolean defaults, @Nonnull String permissionGroup) {
            super(name, description);
            this.visible = visible;
            this.defaults = defaults;
            setPermissionGroups(permissionGroup);
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            apply(context, defaults, new HudSettings(visible, null, null, null));
        }
    }

    private class MoveCommand extends CommandBase {
        private final OptionalArg<HudSettings.Side> side = withOptionalArg("side", "The edge of the screen it hangs from: Left or Right", ArgTypes.forEnum("side", HudSettings.Side.class));
        private final OptionalArg<Integer> offset = withOptionalArg("offset", "How far from that edge, in UI pixels", ArgTypes.INTEGER);
        private final OptionalArg<Integer> top = withOptionalArg("top", "How far from the top of the screen, in UI pixels", ArgTypes.INTEGER);

        private final boolean defaults;

        MoveCommand(boolean defaults, @Nonnull String permissionGroup) {
            super("move", "Moves the quest tracker, keeping whatever is not given");
            this.defaults = defaults;
            setPermissionGroups(permissionGroup);
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            if (!side.provided(context) && !offset.provided(context) && !top.provided(context)) {
                context.sendMessage(Message.translation("openquests.hud.move-nothing"));
                return;
            }

            apply(context, defaults, new HudSettings(null,
                                                     side.provided(context) ? side.get(context) : null,
                                                     offset.provided(context) ? offset.get(context) : null,
                                                     top.provided(context) ? top.get(context) : null));
        }
    }

    private class ResetCommand extends CommandBase {
        private final boolean defaults;

        ResetCommand(boolean defaults, @Nonnull String permissionGroup) {
            super("reset", defaults ? "Puts the built-in defaults back" : "Puts the quest tracker back to the defaults");
            this.defaults = defaults;
            setPermissionGroups(permissionGroup);
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            apply(context, defaults, null);
        }
    }
}
