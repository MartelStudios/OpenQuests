package com.martelstudios.openquests.core.persistence;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * What a player carries besides their quests, written by the one server hosting them, and the
 * messages other servers leave them meanwhile: a server never writes a record it does not host,
 * it leaves a message the host takes in.
 */
public interface PlayerStore {

    /**
     * @return their record, empty for a player who never held a quest. Never {@code null}.
     */
    @Nonnull
    PlayerQuestRecord loadPlayer(@Nonnull UUID playerId);

    /**
     * Replaces a player's record whole and lets go of the messages it took in, both or neither: a
     * message is never lost nor taken in twice.
     *
     * @param delivered the ids of the messages the record now holds
     */
    void savePlayer(@Nonnull UUID playerId, @Nonnull PlayerQuestRecord record, @Nonnull Collection<UUID> delivered);

    /**
     * Leaves a message for a player, whichever server hosts them, now or later.
     */
    void postMessage(@Nonnull PlayerMessage message);

    /**
     * @return the messages waiting for those players, oldest first.
     */
    @Nonnull
    List<PlayerMessage> loadMessages(@Nonnull Collection<UUID> playerIds);
}
