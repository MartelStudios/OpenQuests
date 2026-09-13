package com.martelstudios.openquests.core.commands.subcommands;

import com.martelstudios.openquests.core.scopes.world.CreateWorldQuestCommand;

import com.martelstudios.openquests.core.scopes.universe.CreateUniverseQuestCommand;

import com.martelstudios.openquests.core.scopes.player.CreatePlayerQuestCommand;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.permissions.provider.HytalePermissionsProvider;

public class CreateQuestCommand extends AbstractCommandCollection {
    public CreateQuestCommand() {
        super("create", "Creates a new quest progression");

        // Named here rather than left to /quest, which is adventurer: creating a progression is the
        // administration this tree is mostly made of. Also covers the three scopes below, since a
        // command reads its parent group only one level up and would otherwise land in no group
        setPermissionGroups(HytalePermissionsProvider.GROUP_WORLD_EDITOR);
        this.addSubCommand(new CreatePlayerQuestCommand());
        this.addSubCommand(new CreateWorldQuestCommand());
        this.addSubCommand(new CreateUniverseQuestCommand());
    }
}
