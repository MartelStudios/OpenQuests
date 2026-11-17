package com.martelstudios.openquests.core.persistence;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.UUID;

/**
 * One team as stored: the authority on who is in it. Never changed once built: a change builds
 * the next document, which the store writes only over the version it was built from.
 */
public final class TeamRecord {

    public static final BuilderCodec<TeamRecord> CODEC = BuilderCodec.builder(TeamRecord.class, TeamRecord::new)
                                                                     .append(new KeyedCodec<>("Id", Codec.UUID_STRING), (team, id) -> team.id = id, team -> team.id)
                                                                     .add()
                                                                     .append(new KeyedCodec<>("Type", Codec.STRING), (team, type) -> team.type = type, team -> team.type)
                                                                     .add()
                                                                     .append(new KeyedCodec<>("Name", Codec.STRING), (team, name) -> team.name = name, team -> team.name)
                                                                     .add()
                                                                     .append(new KeyedCodec<>("Owner", Codec.UUID_STRING), (team, owner) -> team.owner = owner, team -> team.owner)
                                                                     .add()
                                                                     .append(new KeyedCodec<>("Members", new ArrayCodec<>(Codec.UUID_STRING, UUID[]::new)), (team, members) -> team.members = List.of(members), team -> team.members.toArray(UUID[]::new))
                                                                     .add()
                                                                     .append(new KeyedCodec<>("Invitations", new ArrayCodec<>(TeamInvitation.CODEC, TeamInvitation[]::new)), (team, invitations) -> team.invitations = List.of(invitations), team -> team.invitations.toArray(TeamInvitation[]::new))
                                                                     .add()
                                                                     .append(new KeyedCodec<>("Version", Codec.LONG), (team, version) -> team.version = version, team -> Long.valueOf(team.version))
                                                                     .add()
                                                                     .build();

    private UUID id;
    private String type;
    private String name;
    private UUID owner;
    private List<UUID> members = List.of();
    private List<TeamInvitation> invitations = List.of();
    private long version;

    private TeamRecord() {}

    public TeamRecord(@Nonnull UUID id, @Nonnull String type, @Nonnull String name, @Nonnull UUID owner, @Nonnull List<UUID> members, @Nonnull List<TeamInvitation> invitations) {
        this(id, type, name, owner, members, invitations, 0);
    }

    private TeamRecord(@Nonnull UUID id, @Nonnull String type, @Nonnull String name, @Nonnull UUID owner, @Nonnull List<UUID> members, @Nonnull List<TeamInvitation> invitations, long version) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.owner = owner;
        this.members = List.copyOf(members);
        this.invitations = List.copyOf(invitations);
        this.version = version;
    }

    /**
     * @return the id OpenQuests drew for the team, which nothing else derives.
     */
    @Nonnull
    public UUID getId() {
        return id;
    }

    /**
     * @return the id of its team type.
     */
    @Nonnull
    public String getType() {
        return type;
    }

    /**
     * @return what players read, not unique.
     */
    @Nonnull
    public String getName() {
        return name;
    }

    /**
     * @return the member who invites and renames.
     */
    @Nonnull
    public UUID getOwner() {
        return owner;
    }

    /**
     * @return the members in the order they joined, which decides the next owner.
     */
    @Nonnull
    public List<UUID> getMembers() {
        return members;
    }

    /**
     * @return the pending invitations, expired ones included until a change drops them.
     */
    @Nonnull
    public List<TeamInvitation> getInvitations() {
        return invitations;
    }

    /**
     * @return the version the store wrote, which a change must still find to be written.
     */
    public long getVersion() {
        return version;
    }

    /**
     * @return this team under another name, at the same version.
     */
    @Nonnull
    public TeamRecord withName(@Nonnull String name) {
        return new TeamRecord(id, type, name, owner, members, invitations, version);
    }

    /**
     * @return this team with another owner, at the same version.
     */
    @Nonnull
    public TeamRecord withOwner(@Nonnull UUID owner) {
        return new TeamRecord(id, type, name, owner, members, invitations, version);
    }

    /**
     * @return this team with other members, at the same version.
     */
    @Nonnull
    public TeamRecord withMembers(@Nonnull List<UUID> members) {
        return new TeamRecord(id, type, name, owner, members, invitations, version);
    }

    /**
     * @return this team with other invitations, at the same version.
     */
    @Nonnull
    public TeamRecord withInvitations(@Nonnull List<TeamInvitation> invitations) {
        return new TeamRecord(id, type, name, owner, members, invitations, version);
    }

    /**
     * @return this team at that version, for the store writing it.
     */
    @Nonnull
    public TeamRecord withVersion(long version) {
        return new TeamRecord(id, type, name, owner, members, invitations, version);
    }
}
