package com.martelstudios.opendialogues.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.auth.ProfileServiceClient;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.permissions.provider.HytalePermissionsProvider;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.martelstudios.opendialogues.dialogues.OpenDialogueAsset;
import com.martelstudios.opendialogues.history.DialogueHistoryRecorder;
import com.martelstudios.opendialogues.services.DialogueService;

import javax.annotation.Nonnull;

import static com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes.GAME_PROFILE_LOOKUP;
/**
 * {@code /odialogues history}: lets an admin give a player a clean slate, online or not.
 */
public class HistoryCommand extends AbstractCommandCollection {

    public HistoryCommand(@Nonnull DialogueService dialogues, @Nonnull DialogueHistoryRecorder recorder) {
        super("history", "Manages what players did with dialogues");
        setPermissionGroups(HytalePermissionsProvider.GROUP_ADMIN);
        addSubCommand(new ResetCommand(dialogues, recorder));
    }

    private static class ResetCommand extends CommandBase {

        @Nonnull
        private final RequiredArg<ProfileServiceClient.PublicGameProfile> playerArg = withRequiredArg("player", "server.commands.argtype.player.desc", GAME_PROFILE_LOOKUP);

        @Nonnull
        private final OptionalArg<String> dialogueArg = withOptionalArg("dialogue", "Id of the one dialogue to forget, all of them when left out",
            new ChoiceArgType("Dialogue", "Id of a dialogue", sender -> OpenDialogueAsset.getAssetStore().getAssetMap().getAssetMap().keySet().stream().sorted()));

        @Nonnull
        private final DialogueService dialogues;

        @Nonnull
        private final DialogueHistoryRecorder recorder;

        ResetCommand(@Nonnull DialogueService dialogues, @Nonnull DialogueHistoryRecorder recorder) {
            super("reset", "Forgets a player's dialogue openings and answers");
            this.dialogues = dialogues;
            this.recorder = recorder;
        }

        /**
         * Closes the open dialogue on the player's world thread before the reset queues there, so
         * its answers, picked from the old history, cannot be clicked after.
         */
        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            ProfileServiceClient.PublicGameProfile profile = context.get(playerArg);
            String dialogueId = context.get(dialogueArg);
            String name = profile.getUsername();

            PlayerRef online = Universe.get().getPlayer(profile.getUuid());
            World world = online == null || online.getWorldUuid() == null ? null : Universe.get().getWorld(online.getWorldUuid());
            if (world != null) {
                world.execute(() -> {
                    if (dialogueId == null) dialogues.close(online);
                    else dialogues.close(online, dialogueId);
                });
            }

            recorder.reset(profile.getUuid(), dialogueId).whenComplete((cleared, error) -> {
                if (error != null) context.sendMessage(Message.raw("Could not reset the history of " + name + ": " + error.getMessage()));
                else if (dialogueId == null) context.sendMessage(Message.raw(cleared ? "Dialogue history of " + name + " cleared." : name + " has no dialogue history."));
                else context.sendMessage(Message.raw(cleared ? "Dialogue '" + dialogueId + "' cleared for " + name + "." : name + " has no history for '" + dialogueId + "'."));
            });
        }
    }
}
