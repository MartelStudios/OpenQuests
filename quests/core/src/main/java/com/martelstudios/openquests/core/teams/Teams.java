package com.martelstudios.openquests.core.teams;

import com.martelstudios.openquests.core.persistence.IndexStore;
import com.martelstudios.openquests.core.persistence.TeamInvitation;
import com.martelstudios.openquests.core.persistence.TeamRecord;
import com.martelstudios.openquests.core.persistence.TeamStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.UnaryOperator;

/**
 * The team documents and their rules: members, owner, invitations. Knows nothing of quests.
 *
 * <p>Every change reads the team fresh and is written over that version only, so a change another
 * server made in between is read again and kept rather than overwritten.
 */
public class Teams {

    /**
     * Past this many lost writes in a row, something keeps writing the team and giving up is safer.
     */
    private static final int MAX_TRIES = 5;

    private final TeamStore store;
    private final IndexStore indexes;
    private final TeamTypes types;
    private final Clock clock;
    private final Duration invitationLifetime;

    public Teams(@Nonnull TeamStore store, @Nonnull IndexStore indexes, @Nonnull TeamTypes types, @Nonnull Clock clock, @Nonnull Duration invitationLifetime) {
        this.store = store;
        this.indexes = indexes;
        this.types = types;
        this.clock = clock;
        this.invitationLifetime = invitationLifetime;
    }

    /**
     * @param name {@code null} for the default name, read after {@code ownerName}
     * @return a new team of that type, its owner its only member.
     * @throws IllegalArgumentException for a type no pack declares.
     */
    @Nonnull
    public TeamRecord create(@Nonnull String type, @Nonnull UUID owner, @Nonnull String ownerName, @Nullable String name) {
        if (types.get(type) == null) throw new IllegalArgumentException("No team type " + type + " is declared");

        for (int tries = 0; tries < MAX_TRIES; tries++) {
            TeamRecord team = new TeamRecord(UUID.randomUUID(), type, name, ownerName, owner, List.of(owner), List.of());
            if (store.createTeam(team)) return team;
        }
        throw new IllegalStateException("Could not draw a free team id");
    }

    /**
     * @return the team as stored now, or {@code null} once disbanded.
     */
    @Nullable
    public TeamRecord get(@Nonnull UUID teamId) {
        return store.loadTeam(teamId);
    }

    /**
     * Appends the player, and drops their invitation into this team now that it is used.
     *
     * @throws IllegalArgumentException for a team gone or emptied, which is being disbanded.
     */
    @Nonnull
    public TeamRecord addMember(@Nonnull UUID teamId, @Nonnull UUID playerId) {
        TeamRecord team = existing(teamId, change(teamId, current -> {
            // Emptied: being disbanded by the server that emptied it
            if (current.getMembers().isEmpty()) return null;
            List<UUID> members = new ArrayList<>(current.getMembers());
            if (!members.contains(playerId)) members.add(playerId);
            return current.withMembers(members).withInvitations(without(current.getInvitations(), playerId));
        }));

        indexes.removeFromIndex(invitationsKey(playerId), List.of(teamId));
        return team;
    }

    /**
     * Removes the player, the first member left becoming owner if they owned it.
     *
     * @return the team left behind, or {@code null} once nobody is left: the server whose write
     * emptied it deletes it, unless a player joined in between, who keeps it.
     */
    @Nullable
    public TeamRecord removeMember(@Nonnull UUID teamId, @Nonnull UUID playerId) {
        TeamRecord team = change(teamId, current -> {
            List<UUID> members = new ArrayList<>(current.getMembers());
            members.remove(playerId);
            UUID owner = current.getOwner().equals(playerId) && !members.isEmpty() ? members.get(0) : current.getOwner();
            return current.withMembers(members).withOwner(owner);
        });
        if (team == null || !team.getMembers().isEmpty()) return team;

        if (!store.deleteTeam(teamId, team.getVersion())) return store.loadTeam(teamId);
        for (TeamInvitation invitation : team.getInvitations()) {
            indexes.removeFromIndex(invitationsKey(invitation.player()), List.of(teamId));
        }
        return null;
    }

    /**
     * @throws IllegalArgumentException when {@code by} does not own the team.
     */
    @Nonnull
    public TeamRecord rename(@Nonnull UUID teamId, @Nonnull UUID by, @Nonnull String name) {
        return existing(teamId, change(teamId, current -> owned(current, by).withName(name)));
    }

    /**
     * Invites the player, replacing an earlier invitation of theirs into this team.
     *
     * @throws IllegalArgumentException when {@code by} does not own the team.
     */
    @Nonnull
    public TeamRecord invite(@Nonnull UUID teamId, @Nonnull UUID by, @Nonnull UUID playerId) {
        long expiresAt = clock.millis() + invitationLifetime.toMillis();

        TeamRecord team = existing(teamId, change(teamId, current -> {
            // Emptied: being disbanded by the server that emptied it
            if (current.getMembers().isEmpty()) return null;
            List<TeamInvitation> invitations = without(owned(current, by).getInvitations(), playerId);
            invitations.add(new TeamInvitation(playerId, by, expiresAt));
            return current.withInvitations(invitations);
        }));

        indexes.addToIndex(invitationsKey(playerId), List.of(teamId));
        return team;
    }

    /**
     * @return the teams of that type with an invitation to the player still running. Pointers to
     * teams that no longer invite them are dropped on the way.
     */
    @Nonnull
    public List<TeamRecord> invitationsOf(@Nonnull UUID playerId, @Nonnull String type) {
        Set<UUID> teamIds = indexes.loadIndex(invitationsKey(playerId));
        long now = clock.millis();

        List<TeamRecord> inviting = new ArrayList<>();
        List<UUID> stale = new ArrayList<>();
        for (UUID teamId : teamIds) {
            TeamRecord team = store.loadTeam(teamId);
            boolean invites = team != null && team.getInvitations().stream().anyMatch(invitation -> invitation.player().equals(playerId) && invitation.expiresAt() > now);

            if (!invites) stale.add(teamId);
            else if (team.getType().equals(type)) inviting.add(team);
        }

        if (!stale.isEmpty()) indexes.removeFromIndex(invitationsKey(playerId), stale);
        return inviting;
    }

    /**
     * Applies the change to the stored team until a write lands on the version it was read at.
     *
     * @return the team as written, or {@code null} if it is gone.
     */
    @Nullable
    private TeamRecord change(@Nonnull UUID teamId, @Nonnull UnaryOperator<TeamRecord> change) {
        for (int tries = 0; tries < MAX_TRIES; tries++) {
            TeamRecord current = store.loadTeam(teamId);
            if (current == null) return null;

            TeamRecord next = change.apply(current);
            if (next == null) return null;
            if (store.updateTeam(current, next)) return next.withVersion(current.getVersion() + 1);
        }
        throw new IllegalStateException("Team " + teamId + " kept changing under " + MAX_TRIES + " writes");
    }

    @Nonnull
    private static TeamRecord existing(@Nonnull UUID teamId, @Nullable TeamRecord team) {
        if (team == null) throw new IllegalArgumentException("No team " + teamId);
        return team;
    }

    @Nonnull
    private static TeamRecord owned(@Nonnull TeamRecord team, @Nonnull UUID by) {
        if (!team.getOwner().equals(by)) throw new IllegalArgumentException(by + " does not own team " + team.getId());
        return team;
    }

    @Nonnull
    private static List<TeamInvitation> without(@Nonnull List<TeamInvitation> invitations, @Nonnull UUID playerId) {
        List<TeamInvitation> kept = new ArrayList<>(invitations);
        kept.removeIf(invitation -> invitation.player().equals(playerId));
        return kept;
    }

    /**
     * @return the index key pointing at the teams that invited the player.
     */
    @Nonnull
    public static String invitationsKey(@Nonnull UUID playerId) {
        return "team-invitations:" + playerId;
    }
}
