package com.martelstudios.hyquests.core.commands;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.martelstudios.hyquests.core.commands.subcommands.*;

public class QuestCommand extends AbstractCommandCollection {
    public QuestCommand() {
        super("quest", "Displays quests info");
        this.addAliases("q");
        this.addSubCommand(new CreateQuestCommand());
    }
}
