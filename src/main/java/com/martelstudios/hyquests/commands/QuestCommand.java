package com.martelstudios.hyquests.commands;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.martelstudios.hyquests.commands.subcommands.*;

public class QuestCommand extends AbstractCommandCollection {
    public QuestCommand() {
        super("quest", "Displays quests info");
        this.addAliases("q");
        this.addSubCommand(new CreateQuestCommand());
    }
}
