package com.martelstudios.opendialogues.commands.characters;

import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.util.Config;
import com.martelstudios.opendialogues.characters.runtime.CharacterSettings;

import javax.annotation.Nonnull;

import static com.martelstudios.opendialogues.commands.characters.CharacterCommand.key;
import static com.martelstudios.opendialogues.commands.characters.CharacterCommand.text;

/**
 * {@code /oc draws [stop|start]}: whether NPCs draw characters, saved so it survives a restart.
 * The scan keeps running, so resets still settle and traits still apply.
 */
class DrawsCommand extends CommandBase {

    private final Config<CharacterSettings> settings;

    DrawsCommand(@Nonnull Config<CharacterSettings> settings) {
        super("draws", key("draws.desc"));
        this.settings = settings;
        addSubCommand(new Switch("stop", false, settings));
        addSubCommand(new Switch("start", true, settings));
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        context.sendMessage(text(settings.get().isDraws() ? "draws.on" : "draws.off"));
    }

    private static class Switch extends CommandBase {

        private final boolean draws;
        private final Config<CharacterSettings> settings;

        Switch(@Nonnull String name, boolean draws, @Nonnull Config<CharacterSettings> settings) {
            super(name, key("draws." + name + ".desc"));
            this.draws = draws;
            this.settings = settings;
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            settings.get().setDraws(draws);
            settings.save();
            context.sendMessage(text(draws ? "draws.started" : "draws.stopped"));
        }
    }
}
