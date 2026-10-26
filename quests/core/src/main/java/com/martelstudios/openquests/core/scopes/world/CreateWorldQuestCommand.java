package com.martelstudios.openquests.core.scopes.world;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.universe.world.World;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import com.martelstudios.openquests.core.services.QuestProgressionService;

import javax.annotation.Nonnull;

import static com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes.WORLD;
import static com.martelstudios.openquests.core.commands.OpenQuestArgTypes.QUEST_ASSET;

public class CreateWorldQuestCommand extends CommandBase {

    @Nonnull
    private final RequiredArg<OpenQuestAsset> assetArg = withRequiredArg("assetId", "server.commands.openquests.quest.create.assetId.desc", QUEST_ASSET);

    private final RequiredArg<World> worldArg = withRequiredArg("world", "server.commands.openquests.quest.create.world.desc", WORLD);

    private final QuestProgressionService progression;
    private final WorldQuestService worlds;

    public CreateWorldQuestCommand(@Nonnull QuestProgressionService progression, @Nonnull WorldQuestService worlds) {
        super("world", "Creates a new quest progression and adds it to the given world's store");
        this.progression = progression;
        this.worlds = worlds;
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        OpenQuestAsset asset = context.get(assetArg);
        World world = context.get(worldArg);

        var quest = progression.registerQuest(asset);
        context.sendMessage(Message.raw("Created quest " + quest.getId() + " from asset '" + asset.getId() + "'."));
        worlds.addQuest(world, quest.getId());
    }
}
