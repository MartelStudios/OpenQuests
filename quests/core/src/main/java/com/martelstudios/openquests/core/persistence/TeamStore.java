package com.martelstudios.openquests.core.persistence;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;
import java.util.List;
import java.util.function.Function;

/**
 * The teams, one document each. Several servers may change one team, so a change is written only
 * over the version it was built from, and the loser reads again.
 */
public interface TeamStore {

    /** Plans against current documents under the backend's team write lock, then commits documents and notification together. */
    @Nullable
    default PlayerMessage transactTeams(@Nonnull Function<List<TeamRecord>, TeamTransaction> plan) {
        throw new UnsupportedOperationException("This backend does not support durable team moves");
    }

    /** @return current documents, for rebuilding membership without trusting a player's cached record. */
    @Nonnull
    default List<TeamRecord> loadTeams() {
        throw new UnsupportedOperationException("This backend does not support team lookup");
    }

    /** @return authoritative memberships, without scanning every team on an indexed backend. */
    @Nonnull
    default List<TeamRecord> loadTeamsForPlayer(@Nonnull UUID playerId) {
        return loadTeams().stream().filter(team -> team.getMembers().contains(playerId)).toList();
    }

    /** Limits planning to the player's memberships and destination while preserving the same write lock. */
    @Nullable
    default PlayerMessage transactTeams(@Nonnull UUID playerId, @Nullable UUID destination,
                                        @Nonnull Function<List<TeamRecord>, TeamTransaction> plan) {
        return transactTeams(plan);
    }

    /**
     * @return the team as stored now, or {@code null} once disbanded or never created.
     */
    @Nullable
    TeamRecord loadTeam(@Nonnull UUID teamId);

    /**
     * Writes a new team at version 0.
     *
     * @return {@code false} if a team with that id is already stored, which is then left alone.
     */
    boolean createTeam(@Nonnull TeamRecord team);

    /**
     * Writes {@code next} at the version after {@code expected}'s, if the stored team is still at
     * {@code expected}'s.
     *
     * @return {@code false} if another write came in between, or the team is gone.
     */
    boolean updateTeam(@Nonnull TeamRecord expected, @Nonnull TeamRecord next);

    /**
     * Removes the team if it is still at that version: a player who joined it in between keeps it.
     *
     * @return {@code false} if another write came in between; {@code true} once gone, already or now.
     */
    boolean deleteTeam(@Nonnull UUID teamId, long expectedVersion);
}
