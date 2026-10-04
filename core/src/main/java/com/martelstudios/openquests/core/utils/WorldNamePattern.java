package com.martelstudios.openquests.core.utils;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.function.FunctionCodec;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * A world name pattern as every asset naming worlds reads it: the whole name must match, so
 * {@code Dungeon} does not match {@code MyDungeonWorld} and {@code .*Dungeon.*} does. Compiled once
 * as it is decoded, so a player moving is only ever a match against a short string.
 */
public final class WorldNamePattern {

    /**
     * Reads the pattern the way {@link #of} does, so a malformed one decodes too and is refused
     * where the asset holding it can be named.
     */
    public static final Codec<WorldNamePattern> CODEC = new FunctionCodec<>(Codec.STRING, WorldNamePattern::of, WorldNamePattern::getSource);

    @Nonnull
    private final String source;

    @Nullable
    private final Pattern compiled;

    @Nullable
    private final String error;

    private WorldNamePattern(@Nonnull String source, @Nullable Pattern compiled, @Nullable String error) {
        this.source = source;
        this.compiled = compiled;
        this.error = error;
    }

    /**
     * Never throws: a malformed pattern is kept with its error, for the asset to be refused at
     * boot with its name on it rather than by a codec that cannot say which one.
     */
    @Nonnull
    public static WorldNamePattern of(@Nonnull String source) {
        try {
            return new WorldNamePattern(source, Pattern.compile(source), null);
        } catch (PatternSyntaxException e) {
            return new WorldNamePattern(source, null, e.getMessage());
        }
    }

    /**
     * @return the pattern, {@code null} for no source: the many places a pattern is optional.
     */
    @Nullable
    public static WorldNamePattern ofNullable(@Nullable String source) {
        return source == null ? null : of(source);
    }

    /**
     * @return why that pattern does not compile, worded for an asset refused over it, {@code null}
     * when it does or when there is none.
     */
    @Nullable
    public static String findError(@Nullable WorldNamePattern pattern) {
        return pattern == null || pattern.error == null ? null : "WorldNamePattern does not compile: " + pattern.error;
    }

    /**
     * @return the pattern as the asset wrote it, for saving it back and for showing it.
     */
    @Nonnull
    public String getSource() {
        return source;
    }

    /**
     * @return why the pattern does not compile, {@code null} if it does.
     */
    @Nullable
    public String getError() {
        return error;
    }

    /**
     * @return whether the whole name matches. Never for a pattern that does not compile.
     */
    public boolean matches(@Nonnull String worldName) {
        return compiled != null && compiled.matcher(worldName).matches();
    }

    /**
     * Read off the player's own reference rather than their entity, so it holds on any thread.
     *
     * @return {@code false} for a player offline or between worlds, who is in none of them.
     */
    public boolean matchesWorldOf(@Nonnull UUID playerId) {
        String worldName = worldNameOf(playerId);
        return worldName != null && matches(worldName);
    }

    @Nullable
    private static String worldNameOf(@Nonnull UUID playerId) {
        PlayerRef player = Universe.get().getPlayer(playerId);
        if (player == null) return null;

        UUID worldId = player.getWorldUuid();
        if (worldId == null) return null;

        World world = Universe.get().getWorld(worldId);
        return world == null ? null : world.getName();
    }
}
