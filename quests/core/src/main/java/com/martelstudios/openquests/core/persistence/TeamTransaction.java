package com.martelstudios.openquests.core.persistence;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.codec.codecs.set.SetCodec;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

/** A team move and its durable notification, committed together or replayed together after a crash. */
public final class TeamTransaction {
    public static final BuilderCodec<TeamTransaction> CODEC = BuilderCodec.builder(TeamTransaction.class, TeamTransaction::new)
            .append(new KeyedCodec<>("Id", Codec.UUID_STRING), (value, id) -> value.id = id, value -> value.id).documentation("Transaction UUID, generated when omitted.").add()
            .append(new KeyedCodec<>("Teams", new ArrayCodec<>(TeamRecord.CODEC, TeamRecord[]::new)),
                    (value, teams) -> value.teams = List.of(teams), value -> value.teams.toArray(TeamRecord[]::new)).documentation("Team snapshots written by this transaction, empty by default.").add()
            .append(new KeyedCodec<>("Message", PlayerMessage.CODEC),
                    (value, message) -> value.message = message, value -> value.message).documentation("Optional player message queued as part of the transaction.").add()
            .append(new KeyedCodec<>("Added", new MapCodec<>(new SetCodec<>(Codec.UUID_STRING, HashSet<UUID>::new, false), HashMap<String, Set<UUID>>::new)),
                    (value, added) -> value.added = added, value -> value.added).documentation("Player UUIDs added to each membership index key, empty by default.").add()
            .append(new KeyedCodec<>("Removed", new MapCodec<>(new SetCodec<>(Codec.UUID_STRING, HashSet<UUID>::new, false), HashMap<String, Set<UUID>>::new)),
                    (value, removed) -> value.removed = removed, value -> value.removed).documentation("Player UUIDs removed from each membership index key, empty by default.").add().documentation("Stores the related team and membership updates for one transaction.").build();

    private List<TeamRecord> teams = List.of();
    private PlayerMessage message;
    private UUID id = UUID.randomUUID();
    private Map<String, Set<UUID>> added = new HashMap<>();
    private Map<String, Set<UUID>> removed = new HashMap<>();

    private TeamTransaction() {}

    public TeamTransaction(@Nonnull List<TeamRecord> teams, @Nullable PlayerMessage message) {
        this.teams = List.copyOf(teams);
        this.message = message;
    }

    /** @return the complete replacement documents, already carrying their next version. */
    @Nonnull
    public List<TeamRecord> teams() { return teams; }

    /** @return the notification made durable with the documents, or null for an invitation-only update. */
    @Nullable
    public PlayerMessage message() { return message; }

    /** @return a stable journal identity, including an invitation update without a notification. */
    public UUID id() { return id; }

    /** @return whether planning made no changes at all. */
    public boolean isEmpty() { return teams.isEmpty() && message == null && added.isEmpty() && removed.isEmpty(); }

    /** Adds index membership to the same commit as the team documents. */
    public TeamTransaction addToIndex(String key, UUID member) {
        added.computeIfAbsent(key, ignored -> new HashSet<>()).add(member);
        return this;
    }

    /** Removes an index membership only after checking the latest team document under the write lock. */
    public TeamTransaction removeFromIndex(String key, UUID member) {
        removed.computeIfAbsent(key, ignored -> new HashSet<>()).add(member);
        return this;
    }

    /** @return index memberships created with this transaction. */
    public Map<String, Set<UUID>> added() { return added; }

    /** @return index memberships removed with this transaction. */
    public Map<String, Set<UUID>> removed() { return removed; }
}
