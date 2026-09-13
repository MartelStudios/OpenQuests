package com.martelstudios.openquests.core.commands;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.permissions.provider.HytalePermissionsProvider;
import com.martelstudios.openquests.core.commands.subcommands.*;
import com.martelstudios.openquests.core.models.QuestState;

public class QuestCommand extends AbstractCommandCollection {
    public QuestCommand() {
        super("quest", "Displays quests info");
        this.addAliases("q");

        setPermissionGroups(HytalePermissionsProvider.GROUP_ADVENTURER);
        this.addSubCommand(new CreateQuestCommand());
        this.addSubCommand(new QuestStateCommand("complete", "Ends your running quests of a given asset as success", QuestState.SUCCESSFUL, HytalePermissionsProvider.GROUP_WORLD_EDITOR));
        this.addSubCommand(new QuestStateCommand("fail", "Ends your running quests of a given asset as failed", QuestState.FAILED, HytalePermissionsProvider.GROUP_WORLD_EDITOR));
        this.addSubCommand(new QuestStateCommand("abandon", "Ends your running quests of a given asset as abandoned", QuestState.ABANDONED));
    }
}
