package com.martelstudios.openquests.core.teams;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * One team: its type's asset id and its name within that type. A personal team is named after its
 * only member, so it needs no creation and two servers always agree on it.
 */
public record TeamKey(@Nonnull String type, @Nonnull String name) {

    private static final String HOLDER_PREFIX = "team:";
    private static final String MEMBERS_PREFIX = "team-members:";
    private static final String PERSONAL_PREFIX = "~";

    /**
     * Refuses a name a key could not be read back from, and a {@code ~} name that is no personal
     * team's, so an admin cannot name a team into a player's personal one.
     */
    public TeamKey {
        if (type.isEmpty() || type.contains(":")) throw new IllegalArgumentException("Bad team type: " + type);
        if (name.isEmpty() || name.contains(":")) throw new IllegalArgumentException("Bad team name: " + name);
        if (name.startsWith(PERSONAL_PREFIX)) ownerOf(name);
    }

    /**
     * @return the team of that type the player is alone in until they join another.
     */
    @Nonnull
    public static TeamKey personal(@Nonnull String type, @Nonnull UUID playerId) {
        return new TeamKey(type, PERSONAL_PREFIX + playerId);
    }

    /**
     * @return the team that holder key names, {@code null} for the key of another holder.
     */
    @Nullable
    public static TeamKey parseHolderKey(@Nonnull String key) {
        if (!key.startsWith(HOLDER_PREFIX)) return null;

        int split = key.indexOf(':', HOLDER_PREFIX.length());
        if (split < 0) return null;

        try {
            return new TeamKey(key.substring(HOLDER_PREFIX.length(), split), key.substring(split + 1));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * @return the key of the team's running quests and of its assignment records.
     */
    @Nonnull
    public String holderKey() {
        return HOLDER_PREFIX + type + ":" + name;
    }

    /**
     * @return the key the team's members are listed under.
     */
    @Nonnull
    public String membersKey() {
        return MEMBERS_PREFIX + type + ":" + name;
    }

    /**
     * @return whether this is a player's personal team, whose only member is that player.
     */
    public boolean isPersonal() {
        return name.startsWith(PERSONAL_PREFIX);
    }

    /**
     * @return the owner of a personal team, {@code null} for a named one.
     */
    @Nullable
    public UUID owner() {
        return isPersonal() ? ownerOf(name) : null;
    }

    @Nonnull
    private static UUID ownerOf(@Nonnull String name) {
        return UUID.fromString(name.substring(PERSONAL_PREFIX.length()));
    }
}
