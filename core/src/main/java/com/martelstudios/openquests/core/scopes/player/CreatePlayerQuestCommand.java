package com.martelstudios.openquests.core.scopes.player;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.auth.ProfileServiceClient;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import com.martelstudios.openquests.core.services.QuestProgressionService;

import javax.annotation.Nonnull;

import static com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes.GAME_PROFILE_LOOKUP;
import static com.martelstudios.openquests.core.commands.OpenQuestArgTypes.QUEST_ASSET;

public class CreatePlayerQuestCommand extends CommandBase {

    @Nonnull
    private final RequiredArg<OpenQuestAsset> assetArg = withRequiredArg("assetId", "server.commands.openquests.quest.create.assetId.desc", QUEST_ASSET);

    private final RequiredArg<ProfileServiceClient.PublicGameProfile> playerArg = withRequiredArg("player", "server.commands.argtype.player.desc", GAME_PROFILE_LOOKUP);

    public CreatePlayerQuestCommand() {
        super("player", "Creates a new quest progression and adds it to the given player's store");
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        OpenQuestAsset asset = context.get(assetArg);
        ProfileServiceClient.PublicGameProfile profile = context.get(playerArg);

        var quest = QuestProgressionService.get().assignQuest(asset, profile.getUuid());
        if (quest == null) {
            context.sendMessage(Message.raw("The constraints of '" + asset.getId() + "' refuse " + profile.getUsername() + " for now."));
            return;
        }

        context.sendMessage(Message.raw("Created quest " + quest.getId() + " from asset '" + asset.getId() + "'."));
    }
}
