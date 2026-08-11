package com.martelstudios.hyquests.commands.subcommands;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

public class CreateQuestCommand extends AbstractCommandCollection {
    public CreateQuestCommand() {
        super("create", "Creates a new quest progression");
        this.addSubCommand(new CreatePlayerQuestCommand());
        this.addSubCommand(new CreateWorldQuestCommand());
        this.addSubCommand(new CreateUniverseQuestCommand());
    }
}
