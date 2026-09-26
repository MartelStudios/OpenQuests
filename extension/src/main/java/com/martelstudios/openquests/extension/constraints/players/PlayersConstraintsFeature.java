package com.martelstudios.openquests.extension.constraints.players;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.constraints.QuestConstraint;
import com.martelstudios.openquests.extension.journal.QuestPageService;

import javax.annotation.Nonnull;

/**
 * Quests that only move while enough players are around.
 */
public final class PlayersConstraintsFeature {
    public static final String MIN_PLAYERS_ONLINE_TYPE_ID = "MinPlayersOnline";

    private PlayersConstraintsFeature() {}

    /**
     * Registers the constraint under its type id, and how the journal describes it.
     */
    public static void register(@Nonnull JavaPlugin plugin) {
        QuestConstraint.CODEC.register(MIN_PLAYERS_ONLINE_TYPE_ID, MinPlayersOnlineConstraint.class, MinPlayersOnlineConstraint.CODEC);

        QuestPageService.register(new MinPlayersOnlineRenderer());
    }
}
