package com.martelstudios.hyquests.commands.subcommands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.universe.world.World;
import com.martelstudios.hyquests.assets.QuestAsset;
import com.martelstudios.hyquests.services.QuestProgressionService;
import com.martelstudios.hyquests.services.WorldQuestService;

import javax.annotation.Nonnull;

import static com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes.STRING;
import static com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes.WORLD;

public class CreateWorldQuestCommand extends CommandBase {

    @Nonnull
    private final RequiredArg<String> assetIdArg = withRequiredArg("assetId", "server.commands.hyquests.quest.create.assetId.desc", STRING);

    private final RequiredArg<World> worldArg = withRequiredArg("world", "server.commands.hyquests.quest.create.world.desc", WORLD);

    public CreateWorldQuestCommand() {
        super("world", "Creates a new quest progression and adds it to the given world's store");
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        String assetId = context.get(assetIdArg);
        World world = context.get(worldArg);

        QuestAsset asset = QuestAsset.getAsset(assetId);
        if (asset == null) {
            context.sendMessage(Message.raw("No quest asset found with id '" + assetId + "'."));
            return;
        }

        var quest = QuestProgressionService.get().registerQuest(asset);
        context.sendMessage(Message.raw("Created quest " + quest.getId() + " from asset '" + assetId + "'."));
        WorldQuestService.get().addQuest(world, quest.getId());
    }
}
