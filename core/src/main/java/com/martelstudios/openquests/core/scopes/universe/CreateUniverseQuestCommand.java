package com.martelstudios.openquests.core.scopes.universe;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.martelstudios.openquests.core.models.QuestAsset;
import com.martelstudios.openquests.core.services.QuestProgressionService;

import javax.annotation.Nonnull;

import static com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes.STRING;

public class CreateUniverseQuestCommand extends CommandBase {

    @Nonnull
    private final RequiredArg<String> assetIdArg = withRequiredArg("assetId", "server.commands.openquests.quest.create.assetId.desc", STRING);

    public CreateUniverseQuestCommand() {
        super("universe", "Creates a new quest progression and adds it to the universe's store");
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        String assetId = context.get(assetIdArg);

        QuestAsset asset = QuestAsset.getAsset(assetId);
        if (asset == null) {
            context.sendMessage(Message.raw("No quest asset found with id '" + assetId + "'."));
            return;
        }

        var quest = QuestProgressionService.get().registerQuest(asset);
        UniverseQuestService.get().addQuest(quest.getId());
        context.sendMessage(Message.raw("Created quest " + quest.getId() + " from asset '" + assetId + "'."));
    }
}
