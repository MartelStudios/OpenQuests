package com.martelstudios.openquests.core.events;

import com.hypixel.hytale.event.IEvent;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Fired once a player is in a team, its running quests joined, so the HUD, the journal and other
 * packs can react without the team service knowing them.
 */
public class TeamJoinedEvent implements IEvent<UUID> {
    @Nonnull
    private final UUID playerId;
    @Nonnull
    private final UUID teamId;

    public TeamJoinedEvent(@Nonnull UUID playerId, @Nonnull UUID teamId) {
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
