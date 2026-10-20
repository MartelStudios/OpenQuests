package com.martelstudios.openquests.core.commands;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.permissions.provider.HytalePermissionsProvider;
import com.martelstudios.openquests.core.OpenQuestsServices;
import com.martelstudios.openquests.core.commands.subcommands.*;
import com.martelstudios.openquests.core.models.QuestState;

import javax.annotation.Nonnull;

public class QuestCommand extends AbstractCommandCollection {
    public QuestCommand(@Nonnull OpenQuestsServices services) {
        super("oquest", "Displays quests info");
        this.addAliases("quest", "q");

        setPermissionGroups(HytalePermissionsProvider.GROUP_ADVENTURER);
        this.addSubCommand(new CreateQuestCommand(services));
        this.addSubCommand(new QuestStateCommand(services.getQuestProgressionService(), services.getPlatform().components(), "complete", "Ends your running quests of a given asset as success", QuestState.SUCCESSFUL, HytalePermissionsProvider.GROUP_WORLD_EDITOR));
        this.addSubCommand(new QuestStateCommand(services.getQuestProgressionService(), services.getPlatform().components(), "fail", "Ends your running quests of a given asset as failed", QuestState.FAILED, HytalePermissionsProvider.GROUP_WORLD_EDITOR));
        this.addSubCommand(new QuestStateCommand(services.getQuestProgressionService(), services.getPlatform().components(), "abandon", "Ends your running quests of a given asset as abandoned", QuestState.ABANDONED));
    }
}
