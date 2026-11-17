package com.martelstudios.openquests.core.events;

import com.hypixel.hytale.event.IEvent;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Fired once a player left a team, its running quests left, so the HUD, the journal and other
 * packs can react without the team service knowing them.
 */
public class TeamLeftEvent implements IEvent<UUID> {
    @Nonnull
    private final UUID playerId;
    @Nonnull
    private final UUID teamId;

    public TeamLeftEvent(@Nonnull UUID playerId, @Nonnull UUID teamId) {
        this.playerId = playerId;
        this.teamId = teamId;
    }

    /**
     * @return the player who joined or left.
     */
    @Nonnull
    public UUID getPlayerId() {
        return playerId;
    }

    /**
     * @return the team joined or left.
     */
    @Nonnull
    public UUID getTeamId() {
        return teamId;
    }
}
