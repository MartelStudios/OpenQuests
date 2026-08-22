package com.martelstudios.openquests.core.commands.subcommands;

import com.martelstudios.openquests.core.scopes.world.CreateWorldQuestCommand;

import com.martelstudios.openquests.core.scopes.universe.CreateUniverseQuestCommand;

import com.martelstudios.openquests.core.scopes.player.CreatePlayerQuestCommand;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

public class CreateQuestCommand extends AbstractCommandCollection {
    public CreateQuestCommand() {
        super("create", "Creates a new quest progression");
        this.addSubCommand(new CreatePlayerQuestCommand());
        this.addSubCommand(new CreateWorldQuestCommand());
        this.addSubCommand(new CreateUniverseQuestCommand());
    }
}
