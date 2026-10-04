package com.martelstudios.openquests.core.replication;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Who holds a quest, as several servers see it: every player's latest move wins, stamped with
 * when and where it was made, so two copies merge into the same answer whatever order they meet
 * in (a last-writer-wins map). A player acts from one server at a time, so two moves of one player
 * never really race.
 */
public final class Membership {

    /**
     * Where a player stands with the quest.
     */
    public enum Status {
        /**
         * Running it.
         */
        JOINED,

        /**
         * Out of it for now, a world quest left behind with its world.
         */
        LEFT,

        /**
         * Gave it up.
         */
        ABANDONED
    }

    /**
     * Written as the map itself, {@code { "<player>": { "Status": …, "At": …, "By": … } }}.
     */
    public static final Codec<Map<String, Move>> CODEC = new MapCodec<>(Move.CODEC, HashMap::new);

    private final Map<UUID, Move> moves = new ConcurrentHashMap<>();

    private final Set<UUID> players = ConcurrentHashMap.newKeySet();

    private final Set<UUID> abandoned = ConcurrentHashMap.newKeySet();

    /**
     * @return the players running the quest, read only.
     */
    @Nonnull
    public Set<UUID> getPlayers() {
        return Collections.unmodifiableSet(players);
    }

    /**
     * @return the players who gave it up, read only.
     */
    @Nonnull
    public Set<UUID> getAbandoned() {
        return Collections.unmodifiableSet(abandoned);
    }

    /**
     * @return whether the player's latest move was that one.
     */
    public boolean is(@Nonnull UUID playerId, @Nonnull Status status) {
        Move move = moves.get(playerId);
        return move != null && move.status == status;
    }

    /**
     * Records a move made on this server, ordered after the player's previous one even if the
     * clock went back.
     *
     * @return whether it changed where the player stands.
     */
    public boolean move(@Nonnull UUID playerId, @Nonnull Status status) {
        Move previous = moves.get(playerId);
        if (previous != null && previous.status == status) return false;

        long at = Math.max(System.currentTimeMillis(), previous == null ? 0 : previous.at + 1);
        apply(playerId, new Move(status, at, Replica.localId()));
        return true;
    }

    /**
     * Takes in what another copy knows: for each player, the later move.
     *
     * @return whether anyone here moved.
     */
    public boolean merge(@Nonnull Membership other) {
        boolean changed = false;
        for (Map.Entry<UUID, Move> move : other.moves.entrySet()) {
            Move mine = moves.get(move.getKey());
            if (mine != null && !move.getValue().isAfter(mine)) continue;

            apply(move.getKey(), move.getValue());
            changed |= mine == null || mine.status != move.getValue().status;
        }
        return changed;
    }

    /**
     * @return the moves by player id as text, for the codec.
     */
    @Nonnull
    public Map<String, Move> toMap() {
        Map<String, Move> map = new HashMap<>();
        moves.forEach((playerId, move) -> map.put(playerId.toString(), move));
        return map;
    }

    /**
     * Reads moves back, keeping the later one where a player already has a move.
     */
    public void putAll(@Nonnull Map<String, Move> map) {
        Membership read = new Membership();
        map.forEach((playerId, move) -> read.moves.put(UUID.fromString(playerId), move));
        merge(read);
    }

    /**
     * Reads back players written as plain lists, older than any move: whatever a copy with moves
     * says of them wins.
     */
    public void putPlain(@Nonnull Collection<UUID> joined, @Nonnull Collection<UUID> gaveUp) {
        Membership read = new Membership();
        for (UUID playerId : joined) read.moves.put(playerId, new Move(Status.JOINED, 0, ""));
        for (UUID playerId : gaveUp) read.moves.put(playerId, new Move(Status.ABANDONED, 0, ""));
        merge(read);
    }

    private void apply(@Nonnull UUID playerId, @Nonnull Move move) {
        moves.put(playerId, move);

        if (move.status == Status.JOINED) players.add(playerId); else players.remove(playerId);
        if (move.status == Status.ABANDONED) abandoned.add(playerId); else abandoned.remove(playerId);
    }

    /**
     * One move of one player: what, when, and on which server, the last breaking ties.
     */
    public static final class Move {

        public static final BuilderCodec<Move> CODEC = BuilderCodec.builder(Move.class, Move::new)
                                                                   .append(new KeyedCodec<>("Status", new EnumCodec<>(Status.class)), (move, status) -> move.status = status, move -> move.status)
                                                                   .add()
                                                                   .append(new KeyedCodec<>("At", Codec.LONG), (move, at) -> move.at = at, move -> Long.valueOf(move.at))
                                                                   .add()
                                                                   .append(new KeyedCodec<>("By", Codec.STRING), (move, by) -> move.by = by, move -> move.by)
                                                                   .add()
                                                                   .build();

        private Status status;

        private long at;

        @Nullable
        private String by;

        private Move() {}

        private Move(@Nonnull Status status, long at, @Nonnull String by) {
            this.status = status;
            this.at = at;
            this.by = by;
        }

        private boolean isAfter(@Nonnull Move other) {
            if (at != other.at) return at > other.at;
            return String.valueOf(by).compareTo(String.valueOf(other.by)) > 0;
        }
    }
}
