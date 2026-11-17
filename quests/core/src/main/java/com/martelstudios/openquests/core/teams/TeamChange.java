package com.martelstudios.openquests.core.teams;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.martelstudios.openquests.core.models.AssignmentRecord;
import com.martelstudios.openquests.core.models.AssignmentRecords;
import com.martelstudios.openquests.core.persistence.QuestProgressionRecord;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.scopes.team.TeamQuestScope;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;
import java.util.List;
import java.util.Map;

/**
 * One membership change of one player, the team documents already written: the team of a type
 * they left and the one they joined, either missing. Applied the same way whoever made it. A class
 * rather than a record, as a codec builds it field by field.
 */
public final class TeamChange {

    public static final BuilderCodec<TeamChange> CODEC = BuilderCodec.builder(TeamChange.class, TeamChange::new)
                                                                     .append(new KeyedCodec<>("Type", Codec.STRING), (change, type) -> change.type = type, change -> change.type)
                                                                     .add()
                                                                     .append(new KeyedCodec<>("From", Codec.UUID_STRING), (change, from) -> change.from = from, change -> change.from)
                                                                     .add()
                                                                     .append(new KeyedCodec<>("To", Codec.UUID_STRING), (change, to) -> change.to = to, change -> change.to)
                                                                     .add()
                                                                     .append(new KeyedCodec<>("Revision", Codec.LONG), (change, revision) -> change.revision = revision, change -> change.revision).add()
                                                                     .append(new KeyedCodec<>("Sources", new ArrayCodec<>(Codec.UUID_STRING, UUID[]::new)), (change, sources) -> change.sources = List.of(sources), change -> change.sources.toArray(UUID[]::new)).add()
                                                                     .append(new KeyedCodec<>("Copies", new ArrayCodec<>(QuestProgressionRecord.CODEC, QuestProgressionRecord[]::new)), (change, copies) -> change.copies = List.of(copies), change -> change.copies.toArray(QuestProgressionRecord[]::new)).add()
                                                                     .append(new KeyedCodec<>("Assignments", AssignmentRecords.MAP_CODEC), (change, records) -> change.assignments = records, change -> change.assignments).add()
                                                                     .build();

    private String type;
    private UUID from;
    private UUID to;
    private long revision;
    private List<UUID> sources = List.of();
    private List<QuestProgressionRecord> copies = List.of();
    private Map<String, Map<String, AssignmentRecord>> assignments = Map.of();

    private TeamChange() {}

    public TeamChange(@Nonnull String type, @Nullable UUID from, @Nullable UUID to) {
        this.type = type;
        this.from = from;
        this.to = to;
    }

    /** Carries the departing player's progress durably, before their old team can be disbanded. */
    public TeamChange(String type, List<UUID> sources, UUID to, List<QuestProgressionRecord> copies,
                      Map<String, Map<String, AssignmentRecord>> assignments) {
        this(type, sources.isEmpty() ? null : sources.getFirst(), to);
        this.sources = List.copyOf(sources);
        this.copies = List.copyOf(copies);
        if (to != null) for (QuestProgressionRecord snapshot : this.copies) {
            if (snapshot.quest.getParentId() == null) snapshot.quest.apply(new AbstractQuestProgression.SetScope(new TeamQuestScope(to)));
        }
        this.assignments = assignments;
    }

    /** @return every team left, including memberships hidden by an outdated player cache. */
    public List<UUID> sources() { return sources.isEmpty() && from != null ? List.of(from) : sources; }

    /** @return the fixed progression snapshots needed to recover a personal departure. */
    public List<QuestProgressionRecord> copies() { return copies; }

    /** @return the assignment history the departing player's new team inherits. */
    public Map<String, Map<String, AssignmentRecord>> assignments() { return assignments; }

    /** @return the backend order, so a delayed message cannot replace a newer cached membership. */
    public long revision() { return revision; }

    /** Set by the backend while holding its team write lock. */
    public void setRevision(long revision) { this.revision = revision; }

    /**
     * @return the id of the team type both teams are of.
     */
    @Nonnull
    public String type() {
        return type;
    }

    /**
     * @return the team left, {@code null} for a player who had none of that type.
     */
    @Nullable
    public UUID from() {
        return from;
    }

    /**
     * @return the team joined, {@code null} for a player left with none of that type.
     */
    @Nullable
    public UUID to() {
        return to;
    }
}
