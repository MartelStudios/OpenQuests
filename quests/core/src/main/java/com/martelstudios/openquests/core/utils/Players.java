package com.martelstudios.openquests.core.utils;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * The players the services reach, online or not. The server answers through its universe; a test
 * through players it keeps in memory.
 */
public interface Players {

    /**
     * @return whether the player is connected to this server.
     */
    boolean isOnline(@Nonnull UUID playerId);

    /**
     * @return the players connected to this server.
     */
    @Nonnull
    Collection<UUID> online();

    /**
     * Runs where a write to the player's components survives: their world thread, their holder, or
     * a pass over their stored data. Asynchronous in every case but the holder.
     */
    void update(@Nonnull UUID playerId, @Nonnull Consumer<EntityComponents> action);
}
