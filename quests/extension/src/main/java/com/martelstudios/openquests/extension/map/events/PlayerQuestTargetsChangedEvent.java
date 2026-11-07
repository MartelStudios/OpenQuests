package com.martelstudios.openquests.extension.map.events;

import com.hypixel.hytale.event.IEvent;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Fired when what a player's running quests lead to may have changed: a quest added, ended,
 * abandoned, tracked or untracked. Whatever shows those targets computes them again. Keyed by
 * nothing, as listeners care about every player.
 */
public class PlayerQuestTargetsChangedEvent implements IEvent<Void> {

    @Nonnull
    private final UUID playerId;

    public PlayerQuestTargetsChangedEvent(@Nonnull UUID playerId) {
        this.playerId = playerId;
    }

    /**
     * @return the player whose targets changed, possibly offline.
     */
    @Nonnull
    public UUID getPlayerId() {
        return playerId;
    }
}
