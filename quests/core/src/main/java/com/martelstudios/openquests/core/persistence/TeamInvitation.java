package com.martelstudios.openquests.core.persistence;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.UUID;

/**
 * A pending invitation into a team, kept in the team's document so it reaches the player on any
 * server. A class rather than a record, as a codec builds it field by field.
 */
public final class TeamInvitation {

    public static final BuilderCodec<TeamInvitation> CODEC = BuilderCodec.builder(TeamInvitation.class, TeamInvitation::new)
                                                                         .append(new KeyedCodec<>("Player", Codec.UUID_STRING), (invitation, id) -> invitation.player = id, invitation -> invitation.player)
                                                                         .documentation("UUID of the invited player.")
                                                                         .add()
                                                                         .append(new KeyedCodec<>("InvitedBy", Codec.UUID_STRING), (invitation, id) -> invitation.invitedBy = id, invitation -> invitation.invitedBy)
                                                                         .documentation("UUID of the player who sent the invitation.")
                                                                         .add()
                                                                         .append(new KeyedCodec<>("ExpiresAt", Codec.LONG), (invitation, at) -> invitation.expiresAt = at, invitation -> Long.valueOf(invitation.expiresAt))
                                                                         .documentation("Time after which the invitation expires.")
                                                                         .add()
                                                                         .documentation("Stores an invitation for a player to join a team.")
                                                                         .build();

    private UUID player;
    private UUID invitedBy;
    private long expiresAt;

    private TeamInvitation() {}

    public TeamInvitation(@Nonnull UUID player, @Nonnull UUID invitedBy, long expiresAt) {
        this.player = player;
        this.invitedBy = invitedBy;
        this.expiresAt = expiresAt;
    }

    /**
     * @return the invited player.
     */
    @Nonnull
    public UUID player() {
        return player;
    }

    /**
     * @return the owner who sent it, named when the player picks between invitations.
     */
    @Nonnull
    public UUID invitedBy() {
        return invitedBy;
    }

    /**
     * @return when it lapses, in epoch milliseconds.
     */
    public long expiresAt() {
        return expiresAt;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof TeamInvitation invitation && expiresAt == invitation.expiresAt && Objects.equals(player, invitation.player) && Objects.equals(invitedBy, invitation.invitedBy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(player, invitedBy, expiresAt);
    }
}
