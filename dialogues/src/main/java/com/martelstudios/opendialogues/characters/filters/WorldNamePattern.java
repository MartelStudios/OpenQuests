package com.martelstudios.opendialogues.characters.filters;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.function.FunctionCodec;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * A world name pattern matched against the whole name, so {@code Dungeon} does not match
 * {@code MyDungeonWorld} and {@code .*Dungeon.*} does. Same reading as OpenQuests' own, which this
 * mod cannot depend on.
 */
public final class WorldNamePattern {

    /**
     * Decodes a malformed pattern too, so the asset holding it is refused with its name on it.
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
     * Never throws: a malformed pattern is kept with its error.
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
     * @return the pattern as the asset wrote it.
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
}
