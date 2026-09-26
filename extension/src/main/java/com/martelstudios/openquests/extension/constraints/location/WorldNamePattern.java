package com.martelstudios.openquests.extension.constraints.location;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * A world name pattern as the constraints of this package read it: the whole name must match, the
 * way {@code EnterWorld} reads its own. Compiled once as it is decoded, so a player moving is only
 * ever a match against a short string.
 */
final class WorldNamePattern {

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
    static WorldNamePattern of(@Nonnull String source) {
        try {
            return new WorldNamePattern(source, Pattern.compile(source), null);
        } catch (PatternSyntaxException e) {
            return new WorldNamePattern(source, null, e.getMessage());
        }
    }

    @Nonnull
    String getSource() {
        return source;
    }

    /**
     * @return why the pattern does not compile, {@code null} if it does.
     */
    @Nullable
    String getError() {
        return error;
    }

    boolean matches(@Nonnull String worldName) {
        return compiled != null && compiled.matcher(worldName).matches();
    }

    /**
     * Read off the player's own reference rather than their entity, so it holds on any thread.
     *
     * @return {@code false} for a player offline or between worlds, who is in none of them.
     */
    boolean matchesWorldOf(@Nonnull UUID playerId) {
        String worldName = worldNameOf(playerId);
        return worldName != null && matches(worldName);
    }

    @Nullable
    static String worldNameOf(@Nonnull UUID playerId) {
        PlayerRef player = Universe.get().getPlayer(playerId);
        if (player == null) return null;

        UUID worldId = player.getWorldUuid();
        if (worldId == null) return null;

        World world = Universe.get().getWorld(worldId);
        return world == null ? null : world.getName();
    }
}
