package com.martelstudios.openquests.core.persistence;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * What a player carries besides their quests, written by the one server hosting them, and the
 * messages other servers leave them meanwhile: a server never writes a record it does not host,
 * it leaves a message the host takes in.
 *
 * <p>A player moving between servers is read by the next one once the last one wrote them out and
 * let go, or once it waited long enough: the record is then the next one's, and a write from the
 * last one is refused. Hosting is renewed while it lasts, so a server that stopped answering spares
 * the next one the wait.
 */
public interface PlayerStore {

    /**
     * @return their record, empty for a player who never held a quest, read without hosting them.
     * Never {@code null}.
     */
    @Nonnull
    PlayerQuestRecord loadPlayer(@Nonnull UUID playerId);

    /**
     * Hosts a player on this server, then reads their record: waits a while for the server they
     * come from to write them out and let go, and takes them over once done waiting.
     *
     * @return their record, as the last server hosting them left it. Never {@code null}.
     */
    @Nonnull
    PlayerQuestRecord hostPlayer(@Nonnull UUID playerId);

    /**
     * Replaces a player's record whole and lets go of the messages it took in, both or neither: a
     * message is never lost nor taken in twice. Refused for a player another server hosts now.
     *
     * @param delivered the ids of the messages the record now holds
     * @param leaving whether this is the last write of their stay, after which another server may
     * host them
     * @return {@code false} if another server hosts them, nothing then written.
     */
    boolean savePlayer(@Nonnull UUID playerId, @Nonnull PlayerQuestRecord record, @Nonnull Collection<UUID> delivered, boolean leaving);

    /**
     * Holds on to every player this server hosts for that much longer.
     */
    void renewHosting(@Nonnull Duration validFor);

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
