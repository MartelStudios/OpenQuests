package com.martelstudios.openquests.core.assignments;

import com.hypixel.hytale.server.core.universe.world.World;
import com.martelstudios.openquests.core.utils.EntityComponents;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Something that may hand a quest out, as a trigger saw it: who it concerns, where, and when. Each
 * part may be missing: a connection happens nowhere in particular, a schedule concerns nobody yet.
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
     * @param player the player's components where they can be written right now, their holder
     * while they connect or enter a world.
     */
    @Nonnull
    public static Occasion of(@Nullable UUID playerId, @Nullable EntityComponents player, @Nullable World world, @Nullable String period) {
        return new Occasion(playerId, player, world, period);
    }

    /**
     * @return the player the occasion concerns, {@code null} for one concerning nobody yet.
     */
    @Nullable
    public UUID getPlayerId() {
        return playerId;
    }

    /**
     * @return that player's components, to be read and written on the thread the occasion came on.
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
}
