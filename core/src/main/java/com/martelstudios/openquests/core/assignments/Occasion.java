package com.martelstudios.openquests.core.assignments;

import com.hypixel.hytale.server.core.universe.world.World;
import com.martelstudios.openquests.core.utils.EntityComponents;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Something that may hand a quest out, as a trigger saw it: who it concerns, where, and when. Each
 * part may be missing: a connection happens nowhere in particular, a schedule concerns nobody yet.
 * A player always comes with their components, which is what the factories guarantee.
 */
public final class Occasion {

    @Nullable
    private final UUID playerId;

    @Nullable
    private final EntityComponents player;

    @Nullable
    private final World world;

    @Nullable
    private final String period;

    private Occasion(@Nullable UUID playerId, @Nullable EntityComponents player, @Nullable World world, @Nullable String period) {
        this.playerId = playerId;
        this.player = player;
        this.world = world;
        this.period = period;
    }

    /**
     * @param player the connecting player's holder: they are not online yet
     */
    @Nonnull
    public static Occasion connection(@Nonnull UUID playerId, @Nonnull EntityComponents player) {
        return new Occasion(playerId, player, null, null);
    }

    /**
     * @param player the entering player's holder: they are not in the world's store yet
     */
    @Nonnull
    public static Occasion entry(@Nonnull UUID playerId, @Nonnull EntityComponents player, @Nonnull World world) {
        return new Occasion(playerId, player, world, null);
    }

    /**
     * A period as it runs, concerning nobody in particular.
     *
     * @param start the start of the period, as an ISO instant
     */
    @Nonnull
    public static Occasion period(@Nonnull String start) {
        return new Occasion(null, null, null, start);
    }

    /**
     * @return the same occasion, belonging to the period starting then: a player connecting while
     * it runs.
     */
    @Nonnull
    public Occasion during(@Nonnull String start) {
        return new Occasion(playerId, player, world, start);
    }

    /**
     * @return the player the occasion concerns, {@code null} for one concerning nobody yet.
     */
    @Nullable
    public UUID getPlayerId() {
        return playerId;
    }

    /**
     * @return that player's components, to be read and written on the thread the occasion came
     * on; {@code null} exactly when there is no player.
     */
    @Nullable
    public EntityComponents getPlayer() {
        return player;
    }

    /**
     * @return the world the occasion happens in, {@code null} for one happening nowhere in particular.
     */
    @Nullable
    public World getWorld() {
        return world;
    }

    /**
     * @return the start of the period the occasion belongs to, {@code null} for one that is not
     * timed.
     */
    @Nullable
    public String getPeriod() {
        return period;
    }

    /**
     * @return whether the occasion is a period, which comes back each time a holder is reached
     * during it and must be told apart from the next.
     */
    public boolean isTimed() {
        return period != null;
    }

    /**
     * What tells hand-outs apart for a holder that is the place already: a world, a group of
     * worlds, the server.
     *
     * @return {@code period:<start>} for a timed occasion, {@code once} for any other.
     */
    @Nonnull
    public String timeKey() {
        return period != null ? "period:" + period : "once";
    }

    /**
     * What tells hand-outs apart for a holder that moves, a player: the world entered is part of
     * it.
     *
     * @return {@code world:<uuid>} for an entry, {@link #timeKey()} otherwise.
     */
    @Nonnull
    public String placeKey() {
        return world != null ? "world:" + world.getWorldConfig().getUuid() : timeKey();
    }
}
