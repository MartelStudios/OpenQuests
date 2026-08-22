package com.martelstudios.openquests.core.scopes.player;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.auth.ProfileServiceClient;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.martelstudios.openquests.core.assets.QuestAsset;
import com.martelstudios.openquests.core.services.QuestProgressionService;

import javax.annotation.Nonnull;

import static com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes.GAME_PROFILE_LOOKUP;
import static com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes.STRING;

public class CreatePlayerQuestCommand extends CommandBase {

    @Nonnull
    private final RequiredArg<String> assetIdArg = withRequiredArg("assetId", "server.commands.openquests.quest.create.assetId.desc", STRING);

    private final RequiredArg<ProfileServiceClient.PublicGameProfile> playerArg = withRequiredArg("player", "server.commands.argtype.player.desc", GAME_PROFILE_LOOKUP);

    public CreatePlayerQuestCommand() {
        super("player", "Creates a new quest progression and adds it to the given player's store");
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        String assetId = context.get(assetIdArg);
        ProfileServiceClient.PublicGameProfile profile = context.get(playerArg);

        QuestAsset asset = QuestAsset.getAsset(assetId);
        if (asset == null) {
            context.sendMessage(Message.raw("No quest asset found with id '" + assetId + "'."));
            return;
        }

        var quest = QuestProgressionService.get().registerQuest(asset);
        quest.addPlayer(profile.getUuid());
        context.sendMessage(Message.raw("Created quest " + quest.getId() + " from asset '" + assetId + "'."));
    }
}
