package com.martelstudios.openquests.extension.rewards.command;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.console.ConsoleSender;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.martelstudios.openquests.core.history.models.QuestHistoryRecord;
import com.martelstudios.openquests.core.rewards.QuestReward;
import com.martelstudios.openquests.core.utils.EntityComponents;

import javax.annotation.Nonnull;

/**
 * Runs a server or player command on completion.
 */
public class CommandReward extends QuestReward {
    public static final BuilderCodec<CommandReward> CODEC = BuilderCodec.builder(CommandReward.class, CommandReward::new)
                                                                        .append(new KeyedCodec<>("Command", Codec.STRING, true), (reward, command) -> reward.command = command, reward -> reward.command)
                                                                        .addValidator(Validators.nonNull())
                                                                        .add()
                                                                        .append(new KeyedCodec<>("AsPlayer", Codec.BOOLEAN), (reward, asPlayer) -> reward.asPlayer = asPlayer, reward -> Boolean.valueOf(reward.asPlayer))
                                                                        .add()
                                                                        .build();

    protected String command;

    protected boolean asPlayer;

    private CommandReward() {}

    @Override
    public boolean grant(@Nonnull QuestHistoryRecord questHistoryRecord, @Nonnull EntityComponents playerComponents) {
        PlayerRef playerRef = playerComponents.getComponent(PlayerRef.getComponentType());
        if (playerRef == null) return false;

        String resolved = resolvePlaceholders(command, playerRef);
        CommandSender sender = asPlayer ? playerRef : ConsoleSender.INSTANCE;

        CommandManager.get().handleCommand(sender, resolved);

        return true;
    }

    /**
     * Supports {@code {player}}, the username of the player. Add a replacement here to support
     * another placeholder.
     *
     * @return the command without its leading slash, which {@link CommandManager} does not expect.
     */
    @Nonnull
    protected String resolvePlaceholders(@Nonnull String command, @Nonnull PlayerRef playerRef) {
        String resolved = command.replace("{player}", playerRef.getUsername());

        return resolved.startsWith("/") ? resolved.substring(1) : resolved;
    }

    public String getCommand() {
        return command;
    }

    public boolean isAsPlayer() {
        return asPlayer;
    }
}
