package com.martelstudios.openquests.core.scopes.universe;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import com.martelstudios.openquests.core.services.QuestProgressionService;

import javax.annotation.Nonnull;

import static com.martelstudios.openquests.core.commands.OpenQuestArgTypes.QUEST_ASSET;

public class CreateUniverseQuestCommand extends CommandBase {

    @Nonnull
    private final RequiredArg<OpenQuestAsset> assetArg = withRequiredArg("assetId", "server.commands.openquests.quest.create.assetId.desc", QUEST_ASSET);

    private final QuestProgressionService progression;
    private final UniverseQuestService universe;

    public CreateUniverseQuestCommand(@Nonnull QuestProgressionService progression, @Nonnull UniverseQuestService universe) {
        super("universe", "Creates a new quest progression and adds it to the universe's store");
        this.progression = progression;
        this.universe = universe;
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        OpenQuestAsset asset = context.get(assetArg);

        var quest = progression.registerQuest(asset);
        universe.addQuest(quest.getId());
        context.sendMessage(Message.raw("Created quest " + quest.getId() + " from asset '" + asset.getId() + "'."));
    }
}
