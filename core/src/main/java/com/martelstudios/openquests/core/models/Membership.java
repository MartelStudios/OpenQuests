package com.martelstudios.openquests.core.models;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Who holds a quest: where each player who ever took it stands with it.
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
     * Written as the map itself, {@code { "<player>": "JOINED" }}.
     */
    public static final Codec<Map<String, Status>> CODEC = new MapCodec<>(new EnumCodec<>(Status.class), HashMap::new);

    private final Map<UUID, Status> statuses = new ConcurrentHashMap<>();

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
     * @return whether the player stands there.
     */
    public boolean is(@Nonnull UUID playerId, @Nonnull Status status) {
        return statuses.get(playerId) == status;
    }

    /**
     * @return whether it changed where the player stands.
     */
    public boolean move(@Nonnull UUID playerId, @Nonnull Status status) {
        if (statuses.put(playerId, status) == status) return false;

        if (status == Status.JOINED) players.add(playerId); else players.remove(playerId);
        if (status == Status.ABANDONED) abandoned.add(playerId); else abandoned.remove(playerId);
        return true;
    }

    /**
     * @return the statuses by player id as text, for the codec.
     */
    @Nonnull
    public Map<String, Status> toMap() {
        Map<String, Status> map = new HashMap<>();
        statuses.forEach((playerId, status) -> map.put(playerId.toString(), status));
        return map;
    }

    /**
     * Reads statuses back.
     */
    public void putAll(@Nonnull Map<String, Status> map) {
        map.forEach((playerId, status) -> move(UUID.fromString(playerId), status));
    }

    /**
     * Reads back players written as plain lists by OpenQuests 3.
     */
    public void putPlain(@Nonnull Collection<UUID> joined, @Nonnull Collection<UUID> gaveUp) {
        for (UUID playerId : joined) move(playerId, Status.JOINED);
        for (UUID playerId : gaveUp) move(playerId, Status.ABANDONED);
    }
}
